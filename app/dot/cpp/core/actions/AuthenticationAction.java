package dot.cpp.core.actions;

import static dot.cpp.core.helpers.CookieHelper.getCookie;

import com.google.gson.JsonObject;
import dot.cpp.core.annotations.Authentication;
import dot.cpp.core.constants.Constants;
import dot.cpp.core.constants.Patterns;
import dot.cpp.core.enums.UserRole;
import dot.cpp.core.exceptions.EntityNotFoundException;
import dot.cpp.core.exceptions.LoginException;
import dot.cpp.core.exceptions.UserException;
import dot.cpp.core.helpers.CookieHelper;
import dot.cpp.core.models.user.entity.User;
import dot.cpp.core.services.LoginService;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.i18n.Messages;
import play.i18n.MessagesApi;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Result;

public class AuthenticationAction extends Action<Authentication> {

  private static final Logger logger = LoggerFactory.getLogger(AuthenticationAction.class);

  @Inject private MessagesApi languageService;
  @Inject private LoginService loginService;

  @Override
  public CompletionStage<Result> call(Request request) {

    final var messages = languageService.preferred(request);
    final var accessToken = CookieHelper.getCookieString(request, Constants.ACCESS_TOKEN);
    final var refreshToken = CookieHelper.getCookieString(request, Constants.REFRESH_TOKEN);
    final var authHeader = request.header(Http.HeaderNames.AUTHORIZATION).orElse("");

    logger.debug("Authentication");
    logger.debug("request: {}", request);
    logger.debug("authHeader: {}", authHeader);
    logger.debug("accessToken: {}", accessToken);
    logger.debug("refreshToken: {}", refreshToken);

    final var constructedAccessToken = constructToken(authHeader, accessToken);
    if (isEmpty(constructedAccessToken) || isInvalidJwt(constructedAccessToken)) {
      logger.warn("Token invalid {}", constructedAccessToken);
      return statusIfPresentOrResult(redirectWithError(messages));
    }
    logger.debug("{}", constructedAccessToken);

    try {
      final var user = loginService.authorizeRequest(accessToken, getConfigUserRoles());
      return delegate.call(request.addAttr(Constants.USER, user));
    } catch (LoginException loginEx) {
      logger.debug("{}", loginEx.getMessage());

      try {
        final var tokens = loginService.refreshTokens(refreshToken);
        final var user =
            loginService.authorizeRequest(
                tokens.get(Constants.ACCESS_TOKEN).getAsString(), getConfigUserRoles());
        return getSuccessfulResult(request, user, tokens);
      } catch (LoginException | UserException | EntityNotFoundException refreshException) {
        return getCompletableFutureResultOnError(messages, refreshException);
      }
    } catch (UserException | EntityNotFoundException userEx) {
      return getCompletableFutureResultOnError(messages, userEx);
    }
  }

  private List<UserRole> getConfigUserRoles() {
    return Arrays.stream(configuration.userRoles()).collect(Collectors.toList());
  }

  private CompletableFuture<Result> getCompletableFutureResultOnError(
      Messages messages, Exception ex) {
    logger.debug("", ex);
    return statusIfPresentOrResult(redirectWithError(messages));
  }

  private CompletionStage<Result> getSuccessfulResult(
      Request request, User user, JsonObject tokens) {
    return delegate
        .call(request.addAttr(Constants.USER, user))
        .thenApply(
            result ->
                result.withCookies(
                    getCookie(
                        Constants.ACCESS_TOKEN, tokens.get(Constants.ACCESS_TOKEN).getAsString()),
                    getCookie(
                        Constants.REFRESH_TOKEN,
                        tokens.get(Constants.REFRESH_TOKEN).getAsString())));
  }

  private String constructToken(
      final String headerAuthorization, final String cookieAuthorization) {
    return isEmpty(headerAuthorization)
        ? cookieAuthorization
        : headerAuthorization.replace("Bearer", "").trim();
  }

  private boolean isInvalidJwt(String token) {
    return !token.matches(Patterns.JWT_TOKEN);
  }

  private CompletableFuture<Result> statusIfPresentOrResult(Result result) {
    final var status = configuration.status();
    if (status != -1) {
      return CompletableFuture.completedFuture(status(status));
    }
    return CompletableFuture.completedFuture(result);
  }

  private Result redirectWithError(Messages messages) {
    logger.debug("Session expired");
    return redirect(configuration.redirectUrl())
        .flashing("alert-danger", messages.apply("general.session.expired"));
  }

  private boolean isEmpty(String string) {
    return string == null || string.isBlank();
  }
}