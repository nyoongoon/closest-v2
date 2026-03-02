/**
 * 블로그 URL에서 파비콘 URL을 생성합니다.
 * Google Favicon API를 사용하여 해당 도메인의 파비콘을 가져옵니다.
 *
 * @param blogUrl - 블로그 URL (e.g. "https://techblog.woowahan.com")
 * @param size - 파비콘 크기 (기본값: 64)
 * @returns 파비콘 URL 문자열
 */
export function getFaviconUrl(blogUrl: string | undefined | null, size: number = 64): string {
  if (!blogUrl) return '';
  try {
    const { hostname } = new URL(blogUrl);
    return `https://www.google.com/s2/favicons?domain=${hostname}&sz=${size}`;
  } catch {
    // URL 파싱 실패 시 blogUrl을 도메인으로 직접 사용
    return `https://www.google.com/s2/favicons?domain=${blogUrl}&sz=${size}`;
  }
}
