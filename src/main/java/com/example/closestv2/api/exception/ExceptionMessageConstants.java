package com.example.closestv2.api.exception;

/**
 * @Valid에 직접 삽입하기 위해 문자열 상수로 선언
 */
public final class ExceptionMessageConstants {
    /* 이메일 */
    public static final String EMAIL_IS_REQUIRED = "이메일은 필수값입니다.";
    public static final String NOT_VALID_EMAIL = "올바른 이메일 형식이 아닙니다.";
    /* 비밀번호 */
    public static final String PASSWORD_IS_REQUIRED = "비밀번호는 필수값입니다.";
    public static final String CONFIRM_PASSWORD_IS_REQUIRED = "확인 비밀번호는 필수값입니다.";
    public static final String NOT_VALID_PASSWORD_FORM = "비밀번호는 대소문자와 특수문자가 하나 이상 씩 포함되어야합니다.";
    public static final String NOT_VALID_CONFIRM_PASSWORD_FORM = "확인 비밀번호는 대소문자와 특수문자가 하나 이상 씩 포함되어야합니다.";
    public static final String NOT_VALID_PASSWORD_SIZE = "비밀번호는 최소 8자 이상 최대 64자 이하입니다";
    public static final String NOT_VALID_CONFIRM_PASSWORD_SIZE = "확인 비밀번호는 최소 8자 이상 최대 64자 이하입니다";
    public static final String NOT_EQUAL_PASSWORDS = "비밀번호와 확인 비밀번호가 다릅니다.";
    public static final String WRONG_PASSWORD = "비밀번호가 일치하지 않습니다.";
    /* 회원 */
    public static final String DUPLICATED_EMAIL = "이미 사용 중인 이메일입니다.";
    public static final String INVALID_MEMBER = "유효하지 않은 회원 정보입니다.";
    public static final String MEMBER_NOT_FOUND = "존재하지 않는 회원입니다.";
    public static final String MEMBER_ID_IS_REQUIRED = "회원 아이디는 필수값입니다.";
    public static final String NICK_NAME_IS_REQUIRED = "닉네임은 필수값입니다.";
    /* 토큰 */
    public static final String EXPIRED_TOKEN = "토큰이 만료되었습니다.";
    /* 나의 블로그 */
    public static final String NOT_EXISTS_MY_BLOG = "나의 블로그가 존재하지 않습니다.";
    public static final String MY_BLOG_URL_IS_REQUIRED = "RSS URL은 필수값입니다.";
    public static final String ALREADY_EXISTS_MY_BLOG = "나의 블로그가 이미 존재합니다.";
    public static final String MY_BLOG_VISIT_COUNT_IS_REQUIRED = "나의 블로그 방문 횟수가 존재하지 않습니다.";
    /* 블로그 */
    public static final String NOT_EXISTS_BLOG = "존재하지 않는 블로그입니다.";
    public static final String BLOG_TITLE_IS_REQUIRED = "블로그명은 필수값입니다.";
    public static final String WRONG_RSS_URL_FORMAT = "RSS URL의 형식이 올바르지 않습니다.";
    public static final String RSS_URL_IS_REQUIRED = "RSS URL은 필수값입니다.";
    public static final String BLOG_URL_IS_REQUIRED = "블로그 URL은 필수값입니다.";
    public static final String BLOG_AUTHOR_IS_REQUIRED = "블로그 저자명은 필수값입니다.";
    public static final String BLOG_PUBLISHED_DATETIME_IS_REQUIRED = "블로그 발행 시간은 필수값입니다.";
    public static final String BLOG_VISIT_COUNT_IS_REQUIRED = "블로그 방문 횟수가 존재하지 않습니다.";
    public static final String BLOG_URL_IS_NOT_CHANGEABLE = "블로그 URL은 변경될 수 없습니다.";
    public static final String BLOG_UPDATABLE_BY_SAME_RSS_URL = "블로그는 같은 RSS URL 정보로 업데이트가 가능합니다";
    public static final String BLOG_UPDATABLE_BY_SAME_BLOG_URL = "블로그는 같은 BLOG URL 정보로 업데이트가 가능합니다";
    public static final String BLOG_NON_UPDATABLE_BY_PAST_PUBLISHED_DATETIME = "블로그는 이전 발행시간 정보로 업데이트할 수 없습니다.";
    /* 포스트 */
    public static final String POST_TITLE_IS_REQUIRED = "포스트명은 필수값입니다.";
    public static final String WRONG_POST_URL_FORMAT = "RSS URL의 형식이 올바르지 않습니다.";
    public static final String POST_URL_IS_REQUIRED = "포스트 URL은 필수값입니다.";
    public static final String NOT_EXISTS_POST_URL = "존재하지 않는 포스트 URL입니다.";
    public static final String POST_PUBLISHED_DATETIME_IS_REQUIRED = "포스트 발행 시간은 필수값입니다.";
    public static final String POST_VISIT_COUNT_IS_REQUIRED = "포스트 방문 횟수가 존재하지 않습니다.";
    /* 구독 */
    public static final String SUBSCRIPTION_VISIT_COUNT_IS_REQUIRED = "포스트 방문 횟수는 필수값입니다.";
    public static final String NEW_POST_COUNT_IS_REQUIRED = "구독의 새 포스트 개수는 필수값입니다.";
    public static final String SUBSCRIPTION_PUBLISHED_DATETIME_IS_REQUIRED = "구독 발생 시간은 필수값입니다.";
    public static final String RECENT_PUBLISHED_DATETIME_IS_PAST = "구독의 최근 발행 시각은 현재 값보다 과거일 수 없습니다.";
    public static final String NOT_FOUND_SUBSCRIPTION = "해당 구독정보를 찾을 수 없습니다.";
    /* 블로그 인증 */
    public static final String FAIL_BLOG_AUTHENTICATE = "블로그 인증에 실패하였습니다.";


    /* 유틸 */
    public static final String CANNOT_INSTANTIATE = "인스턴스화 할 수 없습니다.";

    /* RSS 클라이언트 에러 */
    public static final String RSS_CLIENT_ERROR = "RSS 조회 중 에러가 발생하였습니다.";

    /* 권한 부족 */
    public static final String ACCESS_DENIED_BY_MEMBER_ID = "권한이 부족합니다 - memberId가 일치하지 않음";
    public static final String INVALID_HEADER = "올바르지 못한 헤더 정보입니다.";

    /* 브라우저 에러 */
    public static final String COOKIE_NOT_FOUND = "쿠키를 찾을 수 없습니다";


    /* 미식별 에러 */
    public static final String SERVER_ERROR = "서버 에러가 발생했습니다.";

    private ExceptionMessageConstants() {
        throw new IllegalStateException();
    }

}
