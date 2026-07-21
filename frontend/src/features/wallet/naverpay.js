export const NAVERPAY_SDK_URL = "https://nsp.pay.naver.com/sdk/js/naverpay.min.js";

// 결제 팝업이 반환된 후(returnUrl) merchantPayKey를 다시 알아야 콜백 API를 호출할 수 있는데,
// 팝업은 새 브라우저 컨텍스트라 부모 창의 React state에 직접 접근이 안 된다 - sessionStorage로 넘긴다.
export const NAVERPAY_MERCHANT_PAY_KEY_STORAGE_KEY = "naverpay:merchantPayKey";

// 실제 가맹점 심사 없이 테스트 전용으로만 운영하므로 mode는 항상 development로 고정한다.
export const createNaverPay = () =>
  window.Naver.Pay.create({
    mode: "development",
    clientId: process.env.NEXT_PUBLIC_NAVERPAY_CLIENT_ID,
    chainId: process.env.NEXT_PUBLIC_NAVERPAY_CHAIN_ID,
    payType: "normal",
  });
