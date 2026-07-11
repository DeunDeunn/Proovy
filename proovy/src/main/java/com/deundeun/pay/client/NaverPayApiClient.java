package com.deundeun.pay.client;

import com.deundeun.global.exception.ApiException;
import com.deundeun.global.exception.ErrorCode;
import com.deundeun.pay.config.NaverPayProperties;
import com.deundeun.pay.dto.naverpay.NaverPayApiResponse;
import com.deundeun.pay.dto.naverpay.NaverPayApplyBody;
import com.deundeun.pay.dto.naverpay.NaverPayResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/**
 * 결제 승인(apply)까지 실제 문서 기준으로 구현했다. 취소(cancel) API는 스펙은 확보했지만
 * 아직 쓰는 곳이 없어 구현하지 않았다 (환불 기능 만들 때 같은 패턴으로 추가하면 됨).
 */
@Slf4j
@Component
public class NaverPayApiClient {

    private static final String IDEMPOTENCY_HEADER = "X-NaverPay-Idempotency-Key";

    private final RestClient restClient;

    public NaverPayApiClient(NaverPayProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        // 결제 승인은 완료까지 시간이 걸릴 수 있어 문서 권장대로 60초로 설정
        requestFactory.setReadTimeout(60_000);
        requestFactory.setConnectTimeout(10_000);

        RestClient.Builder builder = RestClient.builder()
                .baseUrl(properties.apiBaseUrl())
                .requestFactory(requestFactory)
                .defaultHeader("X-Naver-Client-Id", properties.clientId())
                .defaultHeader("X-Naver-Client-Secret", properties.clientSecret());

        if (properties.chainId() != null && !properties.chainId().isBlank()) {
            builder.defaultHeader("X-NaverPay-Chain-Id", properties.chainId());
        }

        this.restClient = builder.build();
    }

    /**
     * 단건 결제 승인. paymentId 하나로 네이버페이 서버에 직접 결제 결과를 확인/확정한다.
     * 멱등키는 paymentId 기반 고정값이라, 응답을 못 받아 재시도해도 같은 요청으로 처리된다.
     */
    public NaverPayApplyBody applyPayment(String paymentId) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("paymentId", paymentId);

        // onStatus가 4xx/5xx(응답은 왔지만 에러 상태)는 전부 mapHttpError로 잡아서 던지므로
        // 그 경우엔 별도 try-catch가 필요 없다. 여기서 잡는 ResourceAccessException은 그거랑
        // 달리 "응답 자체를 못 받은" 경우(타임아웃, 연결 실패)라 onStatus가 아예 발동하지 않는다.
        try {
            NaverPayApiResponse<NaverPayApplyBody> response = restClient.post()
                    .uri("/v2.2/apply/payment")
                    .header(IDEMPOTENCY_HEADER, "apply-" + paymentId)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        throw mapHttpError(res.getStatusCode().value());
                    })
                    .body(new ParameterizedTypeReference<NaverPayApiResponse<NaverPayApplyBody>>() {
                    });

            return unwrap(response);
        } catch (ResourceAccessException e) {
            log.error("NaverPay 승인 API 호출 중 타임아웃/연결 오류 - paymentId={}", paymentId, e);
            //프론트 개발 시 자동 재연결
            throw new ApiException(ErrorCode.PG_SERVICE_UNAVAILABLE, "네이버페이 서버 응답이 지연되고 있습니다. 잠시 후 다시 시도해주세요.");
        }
    }

    private <T> T unwrap(NaverPayApiResponse<T> response) {
        if (response == null) {
            throw new ApiException(ErrorCode.PG_REQUEST_FAILED);
        }
        if (!response.isSuccess()) {
            log.warn("NaverPay API 실패 응답 - code={}, message={}", response.code(), response.message());
            throw mapResultCode(response.code(), response.message());
        }
        if (response.body() == null) {
            log.warn("NaverPay API 성공 응답인데 body가 비어있음 - code={}", response.code());
            throw new ApiException(ErrorCode.PG_REQUEST_FAILED, "응답 본문이 비어있습니다.");
        }
        return response.body();
    }

    private ApiException mapResultCode(String code, String message) {
        return switch (code) {
            case NaverPayResultCode.NOT_ENOUGH_ACCOUNT_BALANCE -> new ApiException(ErrorCode.PG_INSUFFICIENT_BALANCE);
            case NaverPayResultCode.TIME_EXPIRED -> new ApiException(ErrorCode.PG_TIME_EXPIRED);
            case NaverPayResultCode.OWNER_AUTH_FAIL -> new ApiException(ErrorCode.PG_OWNER_AUTH_FAIL);
            case NaverPayResultCode.ALREADY_ON_GOING, NaverPayResultCode.ALREADY_COMPLETE ->
                    new ApiException(ErrorCode.PG_DUPLICATE_REQUEST);
            case NaverPayResultCode.BANK_MAINTENANCE, NaverPayResultCode.MAINTENANCE_ONGOING, NaverPayResultCode.FAULT_CHECK_ONGOING ->
                    new ApiException(ErrorCode.PG_SERVICE_UNAVAILABLE);
            case NaverPayResultCode.INVALID_MERCHANT -> new ApiException(ErrorCode.PG_UNAUTHORIZED);
            default -> new ApiException(ErrorCode.PG_REQUEST_FAILED, message);
        };
    }

    private ApiException mapHttpError(int httpStatus) {
        return switch (httpStatus) {
            case 401 -> new ApiException(ErrorCode.PG_UNAUTHORIZED);
            case 404 -> new ApiException(ErrorCode.CHARGE_TRANSACTION_NOT_FOUND);
            case 409 -> new ApiException(ErrorCode.PG_DUPLICATE_REQUEST);
            case 503 -> new ApiException(ErrorCode.PG_SERVICE_UNAVAILABLE);
            default -> new ApiException(ErrorCode.PG_REQUEST_FAILED);
        };
    }
}
