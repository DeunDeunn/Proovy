package com.deundeun.certification.dto;

import lombok.Data;

// 댓글 INSERT용 파라미터. id는 useGeneratedKeys로 DB가 채워준다.
@Data
public class CreateCommentSqlParam {
    private Long id;                    // 생성된 댓글 id (INSERT 후 채워짐)
    private Long postId;
    private Long userId;
    private Long parentCommentId;       // 최상위 댓글이면 null
    private String contents;
}
