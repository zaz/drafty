import { Request, Response, NextFunction } from "express";

/**
 * POST /new-row
 * Add new row
 */
export const postNewRow = (req: Request, res: Response, next: NextFunction) => {
    //check for errors
  // TODO
    return res.sendStatus(200);
};

/**
 * POST /edit
 * Edit
 *
 * @param {number} req.body.idUniqueID
 * @param {number} req.body.idSuggestion
 * @param {string} req.body.value
 */
export const postEdit = (req: Request, res: Response, next: NextFunction) => {
  // TODO
  const edit = req.body.edit;
  console.log(`received user edit: ${edit}`);

  /*
  * Each Edit is one of 3 possible scenarios:
  * 1. The suggested value already exists, therefore we need to increment the confidence
  * 2. It is a new suggestion for that row/column (idUniqueId/idSuggestionType)
  * 3. It is an Alias: the edit is similar to an existing suggestion for that row/column (idUniqueId/idSuggestionType)
  * FINALLY: record the edit happened and return the idSuggestion if #1
  */

  return res.sendStatus(200);
};

/**
 * POST /click
 * Click
 *
 * @param {number} req.body.idSuggestion
 * @param {Array<string>} req.body.rowValues
 */
export const postClick = (req: Request, res: Response, next: NextFunction) => {
    return res.sendStatus(200);
};

/**
 * POST /click-double
 * Double click
 * @param {number} req.body.idSuggestion
 * @param {Array<string>} req.body.rowValues
 */
export const postClickDouble = (req: Request, res: Response, next: NextFunction) => {

  // TODO
    return res.sendStatus(200);

};

/**
 * POST /sort
 * Sort
 */
export const postSort = (req: Request, res: Response, next: NextFunction) => {

  // TODO
    return res.sendStatus(200);
};

/**
 * POST /search-partial
 * Partial search
 */
export const postSearchPartial = (req: Request, res: Response, next: NextFunction) => {

  // TODO
    return res.sendStatus(200);
};

/**
 * POST /search-full
 * Full search
 */
export const postSearchFull = (req: Request, res: Response, next: NextFunction) => {

  // TODO
    return res.sendStatus(200);
};


/**
 * POST /copy-cell
 * Copy
 *
 * @param {number} req.body.idSuggestion
*/
export const postCopyCell = (req: Request, res: Response, next: NextFunction) => {

  // TODO
    return res.sendStatus(200);
};

/**
 * POST /copy-column
 * Copy entire column
 *
 * @param {number} req.body.idSuggestionType
 */
export const postCopyColumn = (req: Request, res: Response, next: NextFunction) => {

  // TODO
    return res.sendStatus(200);
};