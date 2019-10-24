import { db,logDbErr } from "./mysql";
import async from "async";

const stmtInsertSuggestion: string = 'INSERT INTO Suggestions (idSuggestion, idSuggestionType, idUniqueID, idProfile, suggestion, confidence) VALUES (null, ?, ?, ?, ?, ?)'
const stmtInsertUniqueId: string = 'INSERT INTO UniqueId (idUniqueID, active) VALUES (null, 1)'

/**
 * save new suggestion
 */
export async function insertSuggestion(idSuggestionType: Number, idUniqueID: Number, idProfile: Number, suggestion: String, confidence: Number, callback: CallableFunction) {
    try {
        const [results, fields] = await db.query(stmtInsertSuggestion, [idSuggestionType, idUniqueID, idProfile, suggestion, confidence]);
        callback(null, results, fields);
    } catch (error) {
        logDbErr(error, "error during insert interaction", "warn");
        callback(error);
    }
}

/**
 * save new row
 */
export async function insertRowId(callback: CallableFunction) {
    try {
        const [results, fields] = await db.query(stmtInsertUniqueId);
        callback(null, results, fields);
    } catch (error) {
        logDbErr(error, "error during insert row (UniqueID)", "warn");
        callback(error);
    }
}