import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .cors().and()              // enable CORS (hooks into CorsWebFilter)
            .csrf().disable()          // disable CSRF for APIs
            .authorizeRequests()
            .antMatchers(HttpMethod.OPTIONS, "/**").permitAll() // allow preflight
            .anyRequest().permitAll(); // or .authenticated() if using auth
    }
}