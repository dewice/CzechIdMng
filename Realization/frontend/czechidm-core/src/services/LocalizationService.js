import i18next from 'i18next';
import XHR from 'i18next-xhr-backend';
import LanguageDetector from 'i18next-browser-languagedetector';
import Cache from 'i18next-localstorage-cache';

let i18nextInstance = null;

/**
* Provides localization context
* # http://i18next.com/docs/api/#language
*/
export default class LocalizationService {

  /**
  * i18n inicialization
  */
  static init(configLoader, cb) {
    // init localization
    i18nextInstance = i18next
      .use(XHR)
      .use(Cache)
      .use(LanguageDetector)
      .init({
        whitelist: ['cs', 'en'],
        nonExplicitWhitelist: true,
        lowerCaseLng: true,
        fallbackLng: 'cs',
        compatibilityJSON: 'v2', // breaking change with multi plural: https://github.com/i18next/i18next/blob/master/CHANGELOG.md#300

        detection: {
          // order and from where user language should be detected
          order: ['querystring', 'cookie', 'localStorage', 'navigator', 'htmlTag'],
          //
          // keys or params to lookup language from
          lookupQuerystring: 'lng',
          lookupCookie: 'i18next',
          lookupLocalStorage: 'i18nextLng',
          //
          // cache user language on
          caches: ['localStorage', 'cookie'],
          //
          // optional htmlTag with lang attribute, the default is:
          htmlTag: document.documentElement
        },

        // have a common namespace used around the full app
        ns: this._getModuleIdsWithLocales(configLoader),
        defaultNS: 'core',

        debug: false,

        interpolation: {
          escapeValue: false // not needed for react!!
        },

        backend: {
          // path where resources get loaded from
          // Global DEBUG is set in index.js. True is only if is run with watchify task
          loadPath: (window.DEBUG ? 'dist/locales' : 'locales') + '/{{ns}}/{{lng}}.json',
          // your backend server supports multiloading
          // /locales/resources.json?lng=de+en&ns=ns1+ns2
          allowMultiLoading: false,

          // allow cross domain requests
          crossDomain: false
        },

        cache: {
          // turn on or off
          enabled: false,
          // prefix for stored languages
          prefix: 'i18next_res_',
          // expiration
          expirationTime: 24 * 60 * 60 * 1000
        }
      }, (error)=> {
        cb(error);
      });
  }

  /**
   * Find ids modules with defined locales
   */
  static _getModuleIdsWithLocales(configLoader) {
    const moduleIdsWithLocales = [];
    for (const moduleId of configLoader.getEnabledModuleIds()) {
      const descriptor = configLoader.getModuleDescriptor(moduleId);
      if (descriptor.mainLocalePath) {
        moduleIdsWithLocales.push(moduleId);
      }
    }
    return moduleIdsWithLocales;
  }

  /**
   * Returns localized message
   * - for supported options see http://i18next.com/pages/doc_features.html
   *
   * @param  {string} key     localization key
   * @param  {object} options parameters
   * @return {string}         localized message
   */
  static i18n(key, options) {
    if (!i18nextInstance) {
      return undefined;
    }
    return i18nextInstance.t(key, options);
  }

  /**
   * Returns current language
   *
   * @return {string} locale
   */
  static getCurrentLanguage() {
    if (!i18nextInstance) {
      return undefined;
    }
    return i18nextInstance.language;
  }
}

/**
* Simple i18n wrapper
*/
export function i18n(key, options) {
  return LocalizationService.i18n(key, options);
}
