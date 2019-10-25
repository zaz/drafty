import passport from "passport";
import passportLocal from "passport-local";
import { idFieldName, emailFieldName, passwordFieldName } from "../models/user";
import { findUserByField, findUserByFieldResultType } from "../database/user";
import { Request, Response, NextFunction } from "express";
import { comparePassword } from "../util/encrypt";


const LocalStrategy = passportLocal.Strategy;

passport.serializeUser<any, any>((user, done) => {
    done(null, user[idFieldName]);
});

passport.deserializeUser((id: number, done) => {
  // finding the user by ID when deserializing
  findUserByField(idFieldName, id, done);
});


/**
 * Sign in using Email and Password.
 */
passport.use(new LocalStrategy((username, password, done) => {
  // support login with email
  findUserByField(emailFieldName, username, (error: Error, user: findUserByFieldResultType) => {
    if (user == null) {
      return done(null, false, { message: error.message });
    }
    if (!user) {
      // QueryError
      return done(error);
    }

    if (!comparePassword(password, user[passwordFieldName])) {
      return done(null, false, { message: "Incorrect password." });
    }

    return done(null, user);
  });
}));


/**
 * OAuth Strategy Overview
 *
 * - User is already logged in.
 *   - Check if there is an existing account with a provider id.
 *     - If there is, return an error message. (Account merging not supported)
 *     - Else link new OAuth account with currently logged-in user.
 * - User is not logged in.
 *   - Check if it's a returning user.
 *     - If returning user, sign in and we are done.
 *     - Else check if there is an existing account with user's email.
 *       - If there is, return an error message.
 *       - Else create a new account.
 */

/**
 * Login Required middleware.
 */
export const isAuthenticated = (req: Request, res: Response, next: NextFunction) => {
    if (req.isAuthenticated()) {
        return next();
    }
    res.redirect("/login");
};
