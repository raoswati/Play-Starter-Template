package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import models.UserModel
import models.Login
import utils.EncryptionUtility
import models.RegistrationForm
import play.api.i18n.Messages
import models.UserModel
import org.bson.types.ObjectId
import play.api.i18n.Lang
import play.api.Play.current

object UserController extends Controller {
  val minLen = 6;
  val currentUserId = "userId"
  val login = Form(
    Forms.mapping(
      "EmailId" -> email,
      "Password" -> nonEmptyText)(Login.apply)(Login.unapply))
  
      val signUpForm = Form(
    Forms.mapping(
      "EmailId" -> email,
      "Password" -> nonEmptyText(minLength = minLen),
      "ConfirmPassword" -> nonEmptyText)(RegistrationForm.apply)(RegistrationForm.unapply))
      
      
  def signIn() = Action { implicit request =>
    Ok(views.html.user.login(login,  Map("" -> "")))
  }
  
  
    def signup() = Action { implicit request =>
    Ok(views.html.user.signUp(signUpForm, Map("" -> "")))
  }

    /**
     * Authenticate user
     */
 def authenticateUser = Action { implicit request =>
    render {
      case Accepts.Html() =>
        login.bindFromRequest.fold(
          errors => BadRequest(views.html.user.login(errors, Map("error" -> Messages("error")))),
          login => {
            val encryptedPassword = EncryptionUtility.encryptPassword(login.password.trim())
            val userOpt = UserModel.authenticateUserForSignIn(login.emailId.trim(), encryptedPassword)
            userOpt match {
              case None => Redirect("/user/login").flashing("error" -> Messages("invalid Credentials"))
              case Some(user) =>
                val userSession = request.session + ("userId" -> user.id.toString)
                Redirect("/").withSession(userSession)
            }
          })
    }
  }
 
 /**
  * Register User
  */
   def registerUser() = Action { implicit request =>
    signUpForm.bindFromRequest.fold(
      errors => BadRequest(views.html.user.signUp(errors, Map("error" -> Messages("error")))),
      signUpForm => {
        UserModel.findUserByEmail(signUpForm.EmailId) match {
          case None =>
                val encryptedPassword = EncryptionUtility.encryptPassword(signUpForm.Password)
                val user = UserModel(new ObjectId, signUpForm.EmailId, encryptedPassword)
                val userOpt = UserModel.createUser(user)
                userOpt match {
                  case None => Redirect("/").flashing("error" -> Messages("error"))
                  case Some(userId) =>
                    Redirect("/").flashing("success" -> Messages("success"))
                }
          case Some(alreadyExistingUser) =>
            Ok(views.html.user.signUp(UserController.signUpForm.fill(signUpForm), Map("error" -> Messages(": EmailId Already Exist"))))
        }
      })
  }
   
   /**
   * Sign out
   */
  def signOut: Action[play.api.mvc.AnyContent] = Action {
    Redirect("/").withNewSession.withLang(Lang("en"))
  }
}