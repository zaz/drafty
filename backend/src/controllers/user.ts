import async from "async";
import crypto from "crypto";
import moment from "moment";
import passport from "passport";
import { Request, Response, NextFunction } from "express";
import { UserModel, idFieldName, emailFieldName, passwordFieldName, passwordResetToken, passwordResetExpires } from "../models/user";
import { findUserByField, findUserByFieldResultType, createUser, updateUser } from "../database/user";
import { IVerifyOptions } from "passport-local";
import { body, validationResult } from "express-validator";
import { emailAlreadyTaken } from "../validation/validators";
import { encryptPassword } from "../util/encrypt";
import { sendMail } from "../util/email";
import "../config/passport";

/**
 * GET /login
 * Login page
 */
export const getLogin = (req: Request, res: Response) => {
    if (req.user) {
        return res.redirect("/");
    }
    res.render("account/login", {
        title: "Login"
    });
};

/**
 * POST /login
 * Sign in using email and password.
 */
export const postLogin = (req: Request, res: Response, next: NextFunction) => {
    //check for errors
    body("email", "Email is not valid").isEmail()     // eslint-disable-next-line @typescript-eslint/camelcase
                                       .normalizeEmail({ gmail_remove_dots: false });
    body("password", "Password cannot be blank").isLength({min: 1});

    const errors = validationResult(req);

    //if there are error redirect
    if (!errors.isEmpty()) {
        req.flash("errors", errors.array());
        return res.redirect("/login");
    }

    // we're good, do something
    passport.authenticate("local", (err: Error, user: UserModel, info: IVerifyOptions) => {
        if (err) { return next(err); }
        if (!user) {
          // authentication error
          req.flash("errors", {msg: info.message});
          return res.redirect("/login");
        }
        req.login(user, (err) => {
          if (err) { return next(err); }
          req.flash("success", { msg: "Success! You are logged in." });
          res.redirect(req.session.returnTo || "/");
        });
    })(req, res, next);
};

/**
 * GET /logout
 * Log out.
 */
export const logout = (req: Request, res: Response) => {
    req.logout();
    res.redirect("/");
};

/**
 * GET /signup
 * Signup page.
 */
export const getSignup = (req: Request, res: Response) => {
    if (req.user) {
        return res.redirect("/");
    }
    res.render("account/signup", {
        title: "Create Account"
    });
};

/**
 * POST /signup
 * Create a new local account.
 */
export const postSignup = (req: Request, res: Response, next: NextFunction) => {
    //check errors
    body("email", "Email is not valid").isEmail()
                                       .custom(emailAlreadyTaken) // eslint-disable-next-line @typescript-eslint/camelcase
                                       .normalizeEmail({ gmail_remove_dots: false });
    body("password", "Password must be at least 4 characters long").isLength({ min: 4 })
                                                                   .customSanitizer(async password => await encryptPassword(password));
    body("confirmPassword", "Passwords do not match").equals(req.body.password);

    const errors = validationResult(req);

    if (!errors.isEmpty()) {
        req.flash("errors", errors.array());
        return res.redirect("/signup");
    }

    const email: string = req.body.email;
    const password: string = req.body.password;
    // creates new user
    const newUser = {
      [emailFieldName]: email,
      [passwordFieldName]: password,
    };

    createUser(newUser, (err: Error) => {
      if (err) { return next(err); }
      req.logIn(newUser, (err) => {
          if (err) {
              return next(err);
          }
          res.redirect("/");
      });
    });
};

/**
 * GET /account
 * Profile page.
 */
export const getAccount = (req: Request, res: Response) => {
    res.render("account/profile", {
        title: "Account Management"
    });
};

/**
 * POST /account/profile
 * Update profile information.
 */
export const postUpdateProfile = (req: Request, res: Response, next: NextFunction) => {
    body("email", "Please enter a valid email address.").isEmail()
                                                        .custom(emailAlreadyTaken) // eslint-disable-next-line @typescript-eslint/camelcase
                                                        .normalizeEmail({ gmail_remove_dots: false });

    const errors = validationResult(req);

    if (!errors.isEmpty()) {
        req.flash("errors", errors.array());
        return res.redirect("/account");
    }

    const user = req.user as UserModel;
    const userid = user[idFieldName];
    const email: string = req.body.email || "";
    const updatedUser = {
      [emailFieldName]: email,
    };
    updateUser(updatedUser, {[idFieldName]: userid}, (error: Error) => {
      if (error) {
        return next(error);
      }
      req.flash("success", { msg: "Profile information has been updated." });
      res.redirect("/account");
    });
};

/**
 * POST /account/password
 * Update current password.
 */
export const postUpdatePassword = (req: Request, res: Response, next: NextFunction) => {
  body("password", "Password must be at least 4 characters long").isLength({ min: 4 })
                                                                 .customSanitizer(async password => await encryptPassword(password));
    body("confirmPassword", "Passwords do not match").equals(req.body.password);

    const errors = validationResult(req);

    if (!errors.isEmpty()) {
        req.flash("errors", errors.array());
        return res.redirect("/account");
    }

    const user = req.user as UserModel;
    const userid = user[idFieldName];
    const password: string = req.body.password;
    const updatedUser = {
      [passwordFieldName]: password,
    };
    updateUser(updatedUser, {[idFieldName]: userid}, (error: Error) => {
      if (error) {
        return next(error);
      }
      req.flash("success", { msg: "Password has been changed." });
      res.redirect("/account");
    });
};

/**
 * POST /account/delete
 * Delete user account.
 */
// export const postDeleteAccount = (req: Request, res: Response, next: NextFunction) => {
//     const user = req.user as UserModel;
//     const userid = user[idFieldName];
//     deleteUser({[idFieldName]: userid}, (error: Error) => {
//       if (error) { return next(error); }
//       req.logout();
//       req.flash("info", { msg: "Your account has been deleted." });
//       res.redirect("/");
//     });
// };

/**
 * GET /reset/:token
 * Reset Password page.
 */
export const getReset = (req: Request, res: Response, next: NextFunction) => {
  if (req.isAuthenticated()) {
    return res.redirect("/");
  }

  const token = req.params.token;
  findUserByField(passwordResetToken, token, (error: Error, user: findUserByFieldResultType) => {
    if (user == null) {
        req.flash("errors", { msg: "Password reset token is invalid" });
        return res.redirect("/forgot");
    }
    if (!user) {
      return next(error);
    }
    const expiration = user[passwordResetExpires];
    if (moment().isSameOrAfter(expiration)) {
      // reset token has expired
      req.flash("errors", { msg: "Password reset token has expired" });
      return res.redirect("/forgot");
    }

    res.render("account/reset", {
        title: "Password Reset"
    });
  });
};

/**
 * POST /reset/:token
 * Process the reset password request.
 */
export const postReset = (req: Request, res: Response, next: NextFunction) => {
  body("password", "Password must be at least 4 characters long.").isLength({ min: 4 });
  body("confirm", "Passwords must match.").equals(req.body.password);

  const errors = validationResult(req);

  if (!errors.isEmpty()) {
      req.flash("errors", errors.array());
      return res.redirect("back");
  }

  async.waterfall([
    function resetPassword(done: Function) {
      const token = req.params.token;
      findUserByField(passwordResetToken, token, (error: Error, user: findUserByFieldResultType) => {
        if (user == null) {
            req.flash("errors", { msg: "Password reset token is invalid" });
            return res.redirect("back");
        }
        if (!user) {
          return next(error);
        }

        const expiration = user[passwordResetExpires];
        if (moment().isSameOrAfter(expiration)) {
          // reset token has expired
          req.flash("errors", { msg: "Password reset token has expired" });
          return res.redirect("back");
        }

        const password = req.body.password;
        const updatedUser: Partial<UserModel> = {
          [passwordFieldName]: password,
          [passwordResetToken]: null,
          [passwordResetExpires]: null,
        };
        updateUser(updatedUser, {[passwordResetToken]: token}, (error: Error) => {
          if (error) {
            return done(error);
          }
          Object.assign(user, updatedUser);
          req.logIn(user, (err) => {
              done(err, user);
          });
        });
      });
      },
      function sendResetPasswordEmail(user: UserModel, done: Function) {
          const mailOptions = {
              to: user.email,
              from: "express-ts@starter.com",
              subject: "Your password has been changed",
              text: `Hello,\n\nThis is a confirmation that the password for your account ${user.email} has just been changed.\n`
          };
          sendMail(mailOptions, (error: Error) => {
            if (error) {
              req.flash("success", { msg: "Success! Your password has been changed." });
              done(error);
            }
          });
      }
  ], (err) => {
      if (err) { return next(err); }
      res.redirect("/");
  });
};

/**
 * GET /forgot
 * Forgot Password page.
 */
export const getForgot = (req: Request, res: Response) => {
    if (req.isAuthenticated()) {
        return res.redirect("/");
    }
    res.render("account/forgot", {
        title: "Forgot Password"
    });
};

/**
 * POST /forgot
 * Create a random token, then the send user an email with a reset link.
 */
export const postForgot = (req: Request, res: Response, next: NextFunction) => {
  body("email", "Please enter a valid email address.").isEmail() // eslint-disable-next-line @typescript-eslint/camelcase
                                                      .normalizeEmail({ gmail_remove_dots: false });

  const errors = validationResult(req);

  if (!errors.isEmpty()) {
      req.flash("errors", errors.array());
      return res.redirect("/forgot");
  }

  const email = req.body.email;
  async.waterfall([
      function createRandomToken(done: Function) {
          crypto.randomBytes(256, (err, buf) => {
              const token = buf.toString("hex");
              done(err, token);
          });
      },
      function setRandomToken(token: string, done: Function) {
        findUserByField(emailFieldName, email, (error: Error, user: findUserByFieldResultType) => {
          if (user == null) {
              req.flash("errors", { msg: "Account with that email address does not exist." });
              return res.redirect("/forgot");
          }
          if (!user) {
            // QueryError or cannot find user by given email
            return done(error);
          }
          const expiration = moment();
          expiration.hour(expiration.hour() + 1); // 1 hour

          const updatedUser = {
            [passwordResetToken]: token,
            [passwordResetExpires]: expiration.toDate(), // 1 hour
          };
          updateUser(updatedUser, {[emailFieldName]: email}, (error: Error) => {
            if (error) {
              return done(error);
            }
            Object.assign(user, updatedUser);
            done(error, token, user);
          });
        });
      },
      function sendForgotPasswordEmail(token: string, user: UserModel, done: Function) {
        const mailOptions = {
            to: user.email,
            from: "no-reply@drafty.cs.brown.edu",
            subject: "Reset your password on Drafty",
            text: `You are receiving this email because you (or someone else) have requested the reset of the password for your account.\n\n
      Please click on the following link, or paste this into your browser to complete the process:\n\n
      http://${req.headers.host}/reset/${token}\n\n
      If you did not request this, please ignore this email and your password will remain unchanged.\n`
        };
        sendMail(mailOptions, (error: Error) => {
          if (error) {
              req.flash("info", { msg: `An e-mail has been sent to ${user.email} with further instructions.` });
              done(error);
          }
        });
      }
  ], (err) => {
      if (err) { return next(err); }
      res.redirect("/forgot");
  });
};
