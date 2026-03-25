function base64UrlDecode(str) {
    var output = str.replace(/-/g, '+').replace(/_/g, '/');
    while (output.length % 4) {
        output += '=';
    }
    return Buffer.from(output, 'base64').toString('utf8');
}

function jwt_decode(token) {
    try {
        var parts = token.split('.');
        if (parts.length !== 3) {
            return null;
        }
        var payload = JSON.parse(base64UrlDecode(parts[1]));
        return payload;
    } catch (e) {
        return null;
    }
}

function extractUserId(r) {
    var auth_header = r.headersIn['Authorization'];
    if (!auth_header || !auth_header.startsWith('Bearer ')) {
        return null;
    }

    var token = auth_header.substring(7);
    var decoded = jwt_decode(token);
    if (!decoded) {
        return null;
    }

    if (decoded.exp && decoded.exp < Math.floor(Date.now() / 1000)) {
        return null;
    }

    return decoded.userId || decoded.sub || decoded.id || '';
}

function jwt_auth(r) {
    var userId = extractUserId(r);
    if (!userId) {
        r.return(401, JSON.stringify({error: 'Unauthorized: Missing or invalid token'}));
        return;
    }

    r.setVariable('jwt_user_id', String(userId));
    r.internalRedirect('/upstream' + r.uri);
}

export default { jwt_auth };
