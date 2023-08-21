package se.mkk.springboot3oauth2loginkeycloakangular;

import java.security.Principal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:4200")
public class UserController {

    @GetMapping
    public Map<String, String> getUser(HttpServletRequest request, HttpSession session, Principal principal) {
        Map<String, String> rtn = new LinkedHashMap<>();
        rtn.put("session_getMaxInactiveInterval_sec", session.getMaxInactiveInterval() + "s");
        rtn.put("session_getLastAccessedTime",
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z").format(new Date(session.getLastAccessedTime())));
        rtn.put("request_getRemoteUser", request.getRemoteUser());
        rtn.put("request_isUserInRole_USER", Boolean.toString(request.isUserInRole("USER")));
        rtn.put("request_getUserPrincipal_getClass", request.getUserPrincipal().getClass().getName());
        rtn.put("principal_getClass_getName", principal.getClass().getName());
        rtn.put("principal_getName", principal.getName());
        if (principal instanceof OAuth2AuthenticationToken token) {
            List<String> authorities = token.getAuthorities().stream()
                    .map(grantedAuthority -> grantedAuthority.getAuthority()).toList();
            rtn.put("OAuth2AuthenticationToken.getAuthorities()", authorities.toString());
        }
        return rtn;
    }
}
