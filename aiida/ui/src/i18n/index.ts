import { createI18n } from "vue-i18n";

import en from "@/assets/locales/en.json";
import de from "@/assets/locales/de.json";


const messages = {
    en,
    de
}

const i18n = createI18n({
    locale: 'de' || navigator.language.slice(0, 2),
    fallbackLocale: "en",
    legacy: false,
    messages
})

export default i18n;