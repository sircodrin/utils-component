package dot.cpp.core.controllers;

import dot.cpp.core.services.RequestErrorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.data.FormFactory;
import play.i18n.MessagesApi;
import play.mvc.Controller;

public class EntityController extends Controller {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  protected final FormFactory formFactory;
  protected final MessagesApi messagesApi;
  protected final RequestErrorService requestErrorService;

  public EntityController(
      FormFactory formFactory, MessagesApi messagesApi, RequestErrorService requestErrorService) {
    this.formFactory = formFactory;
    this.messagesApi = messagesApi;
    this.requestErrorService = requestErrorService;
  }
}