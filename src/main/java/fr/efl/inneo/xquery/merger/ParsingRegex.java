/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.efl.inneo.xquery.merger;

import java.util.regex.Pattern;

/**
 *
 * @author ext-cmarchand
 */
class ParsingRegex {
    static final Pattern ENCODING_LINE = Pattern.compile("^(xquery +)?encoding +(\"|').+(\"|') ?;$");
    static final Pattern NORMALIZE_SPACE = Pattern.compile("\\p{Space}[\\p{Space}]+");
    static final Pattern QUOTE_EXTRACT = Pattern.compile("[\"|'](.*)[\"|']");
    static final Pattern DECLARE_NAMESPACE_LINE = Pattern.compile("^(declare|module) namespace \\p{Alnum}+=(\"|')\\p{Print}+(\"|') ?;$");
    static final Pattern NAMESPACE_EXTRACT = Pattern.compile("^\\p{Print}+= ?[\"|'](\\p{Print}+)[\"|'].*$");
    static final Pattern PREFIX_EXTRACT = Pattern.compile("^.* (\\p{Graph}+) ?=.*$");
    static final Pattern IMPORT_MODULE_LINE = Pattern.compile("^import module namespace \\p{Print}+= ?(\"|')\\p{Print}+(\"|') at (\"|')\\p{Print}+(\"|') ?;$");
    static final Pattern MODULE_URL_EXTRACT = Pattern.compile(".+at [\"|'](\\p{Print}+)[\"|'].*$");
}
