function saveAuthToCookie(value: string) {
    document.cookie = `accessToken=${value}`;
}

function getAccessTokenFromCookie() {
    return document.cookie.replace(/(?:(?:^|.*;\s*)accessToken\s*=\s*([^;]*).*$)|^.*$/, '$1');
}

function deleteCookieFromBrowser(name: string) {
    document.cookie = name + '=; Max-Age=-99999999; path=/;';
}

export {
    saveAuthToCookie,
    getAccessTokenFromCookie,
    deleteCookieFromBrowser,
}