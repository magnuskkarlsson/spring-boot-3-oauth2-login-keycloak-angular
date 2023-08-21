# Install RH SSO 7.6

    $ unzip rh-sso-7.6.0-server-dist.zip
    $ mv rh-sso-7.6 rh-sso-7.6.0
    
    $ cd rh-sso-7.6.0/bin/
    $ ./add-user-keycloak.sh -u admin
    Press ctrl-d (Unix) or ctrl-z (Windows) to exit
    Password: 
    Added 'admin' to '/home/magnuskkarlsson/bin/rh-sso-7.6/standalone/configuration/keycloak-add-user.json', restart server to load user
    
    $ export JAVA_HOME=/usr/lib/jvm/java-11-openjdk
    $ ./standalone.sh -Djboss.socket.binding.port-offset=100

# Test

http://localhost:8180/

# Configure

Create new realm demo

Create new user john and set Credentials/Password

Create new Role USER

Add Role USER to user john

# Create Spring Boot Project

Create new Spring Boot 3 project with Initializer (https://start.spring.io/) and add dependency

- Spring Web
- Spring Security
- OAuth2 Client
- Spring Boot DevTools

# Configure Spring Security OAuth2 Login

https://docs.spring.io/spring-security/reference/servlet/oauth2/login/core.html#oauth2login-boot-property-mappings

# Angular App

    $ ng new frontend
    ? Would you like to add Angular routing? Yes
    ? Which stylesheet format would you like to use? CSS

$ ng generate component user

$ ng generate component hello

    
    
    
// "TCP and SSL are stateful so your system is stateful whether you knew it or not"
// "The main point to take on board here is that security is stateful. You can’t have a secure,
// stateless application."
// Rob Winch Spring Exchange 2014
// https://spring.io/guides/tutorials/spring-security-and-angular-js/


http://localhost:8180/auth/realms/demo/.well-known/openid-configuration

http://localhost:8180/auth/realms/demo/protocol/openid-connect/logout

Cross-Origin Request Blocked: The Same Origin Policy disallows reading the remote resource at http://localhost:8180/auth/realms/demo/protocol/openid-connect/logout?id_token_hint=ey....loA&post_logout_redirect_uri=http://localhost:8080. (Reason: CORS header ‘Access-Control-Allow-Origin’ missing). Status code: 302.












