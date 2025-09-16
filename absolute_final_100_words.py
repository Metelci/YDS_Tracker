#!/usr/bin/env python3
import json
import os

def load_vocabulary():
    try:
        with open('app/src/main/assets/vocabulary_database.json', 'r', encoding='utf-8') as f:
            return json.load(f)
    except FileNotFoundError:
        return []

def save_vocabulary(data):
    os.makedirs('app/src/main/assets', exist_ok=True)
    with open('app/src/main/assets/vocabulary_database.json', 'w', encoding='utf-8') as f:
        json.dump(data, f, indent=2, ensure_ascii=False)

def add_final_100_words():
    return [
        # Grammar and language structure (30 words)
        {"word": "gerund", "definition": "verb form functioning as noun", "difficulty": 6, "category": "GRAMMAR_FOCUSED", "contexts": ["Gerund phrase", "Use gerund"], "relatedWords": ["verb", "noun", "form"], "grammarPattern": "gerund + form/phrase", "weekIntroduced": 19},
        {"word": "infinitive", "definition": "basic form of verb", "difficulty": 5, "category": "GRAMMAR_FOCUSED", "contexts": ["Infinitive form", "Split infinitive"], "relatedWords": ["verb", "base", "form"], "grammarPattern": "infinitive + form/verb", "weekIntroduced": 17},
        {"word": "participle", "definition": "verb form used as adjective", "difficulty": 6, "category": "GRAMMAR_FOCUSED", "contexts": ["Past participle", "Present participle"], "relatedWords": ["verb", "adjective", "modifier"], "grammarPattern": "past/present + participle", "weekIntroduced": 18},
        {"word": "conjunction", "definition": "word used to connect clauses", "difficulty": 5, "category": "GRAMMAR_FOCUSED", "contexts": ["Coordinating conjunction", "Subordinating conjunction"], "relatedWords": ["connector", "linking", "word"], "grammarPattern": "conjunction + word/phrase", "weekIntroduced": 16},
        {"word": "preposition", "definition": "word expressing relation", "difficulty": 5, "category": "GRAMMAR_FOCUSED", "contexts": ["Preposition phrase", "End with preposition"], "relatedWords": ["relation", "position", "word"], "grammarPattern": "preposition + phrase/of", "weekIntroduced": 15},
        {"word": "adverb", "definition": "word modifying verb or adjective", "difficulty": 4, "category": "GRAMMAR_FOCUSED", "contexts": ["Adverb clause", "Modify with adverb"], "relatedWords": ["modifier", "verb", "adjective"], "grammarPattern": "adverb + clause/form", "weekIntroduced": 14},
        {"word": "adjective", "definition": "word describing noun", "difficulty": 4, "category": "GRAMMAR_FOCUSED", "contexts": ["Adjective clause", "Descriptive adjective"], "relatedWords": ["descriptor", "noun", "modifier"], "grammarPattern": "adjective + clause/form", "weekIntroduced": 13},
        {"word": "pronoun", "definition": "word substituting for noun", "difficulty": 4, "category": "GRAMMAR_FOCUSED", "contexts": ["Personal pronoun", "Relative pronoun"], "relatedWords": ["substitute", "noun", "reference"], "grammarPattern": "pronoun + reference/form", "weekIntroduced": 14},
        {"word": "interjection", "definition": "exclamation expressing emotion", "difficulty": 6, "category": "GRAMMAR_FOCUSED", "contexts": ["Mild interjection", "Express with interjection"], "relatedWords": ["exclamation", "emotion", "expression"], "grammarPattern": "interjection + expressing/of", "weekIntroduced": 20},
        {"word": "clause", "definition": "group of words with subject and verb", "difficulty": 5, "category": "GRAMMAR_FOCUSED", "contexts": ["Independent clause", "Dependent clause"], "relatedWords": ["sentence", "phrase", "group"], "grammarPattern": "clause + structure/type", "weekIntroduced": 16},
        {"word": "phrase", "definition": "group of words without verb", "difficulty": 4, "category": "GRAMMAR_FOCUSED", "contexts": ["Noun phrase", "Prepositional phrase"], "relatedWords": ["group", "words", "expression"], "grammarPattern": "phrase + structure/type", "weekIntroduced": 15},
        {"word": "syntax", "definition": "arrangement of words in sentences", "difficulty": 6, "category": "GRAMMAR_FOCUSED", "contexts": ["Syntax error", "English syntax"], "relatedWords": ["structure", "arrangement", "grammar"], "grammarPattern": "syntax + error/rule", "weekIntroduced": 21},
        {"word": "morpheme", "definition": "smallest meaningful unit of language", "difficulty": 7, "category": "GRAMMAR_FOCUSED", "contexts": ["Bound morpheme", "Free morpheme"], "relatedWords": ["unit", "meaning", "language"], "grammarPattern": "morpheme + analysis/structure", "weekIntroduced": 24},
        {"word": "phoneme", "definition": "smallest unit of sound", "difficulty": 7, "category": "GRAMMAR_FOCUSED", "contexts": ["English phoneme", "Phoneme recognition"], "relatedWords": ["sound", "unit", "pronunciation"], "grammarPattern": "phoneme + recognition/analysis", "weekIntroduced": 25},
        {"word": "syllable", "definition": "unit of pronunciation", "difficulty": 5, "category": "GRAMMAR_FOCUSED", "contexts": ["Stressed syllable", "Count syllables"], "relatedWords": ["sound", "pronunciation", "rhythm"], "grammarPattern": "syllable + stress/count", "weekIntroduced": 17},
        {"word": "prefix", "definition": "element added to beginning of word", "difficulty": 5, "category": "GRAMMAR_FOCUSED", "contexts": ["Common prefix", "Add prefix"], "relatedWords": ["beginning", "addition", "word"], "grammarPattern": "prefix + meaning/form", "weekIntroduced": 16},
        {"word": "suffix", "definition": "element added to end of word", "difficulty": 5, "category": "GRAMMAR_FOCUSED", "contexts": ["Word suffix", "Add suffix"], "relatedWords": ["ending", "addition", "word"], "grammarPattern": "suffix + meaning/form", "weekIntroduced": 16},
        {"word": "root", "definition": "basic form of word", "difficulty": 4, "category": "GRAMMAR_FOCUSED", "contexts": ["Word root", "Latin root"], "relatedWords": ["base", "origin", "foundation"], "grammarPattern": "root + word/meaning", "weekIntroduced": 15},
        {"word": "stem", "definition": "main part of word", "difficulty": 5, "category": "GRAMMAR_FOCUSED", "contexts": ["Word stem", "Verb stem"], "relatedWords": ["base", "core", "foundation"], "grammarPattern": "stem + form/change", "weekIntroduced": 18},
        {"word": "tense", "definition": "form of verb expressing time", "difficulty": 4, "category": "GRAMMAR_FOCUSED", "contexts": ["Past tense", "Future tense"], "relatedWords": ["time", "verb", "form"], "grammarPattern": "tense + form/usage", "weekIntroduced": 14},
        {"word": "aspect", "definition": "grammatical feature expressing time", "difficulty": 6, "category": "GRAMMAR_FOCUSED", "contexts": ["Perfect aspect", "Progressive aspect"], "relatedWords": ["time", "grammar", "completion"], "grammarPattern": "aspect + marker/form", "weekIntroduced": 20},
        {"word": "mood", "definition": "grammatical feature expressing attitude", "difficulty": 6, "category": "GRAMMAR_FOCUSED", "contexts": ["Subjunctive mood", "Indicative mood"], "relatedWords": ["attitude", "grammar", "expression"], "grammarPattern": "mood + form/usage", "weekIntroduced": 21},
        {"word": "voice", "definition": "grammatical feature showing action relationship", "difficulty": 5, "category": "GRAMMAR_FOCUSED", "contexts": ["Active voice", "Passive voice"], "relatedWords": ["action", "relationship", "grammar"], "grammarPattern": "voice + construction/form", "weekIntroduced": 17},
        {"word": "case", "definition": "grammatical category of nouns", "difficulty": 5, "category": "GRAMMAR_FOCUSED", "contexts": ["Nominative case", "Objective case"], "relatedWords": ["noun", "grammar", "function"], "grammarPattern": "case + form/usage", "weekIntroduced": 18},
        {"word": "gender", "definition": "grammatical classification", "difficulty": 4, "category": "GRAMMAR_FOCUSED", "contexts": ["Grammatical gender", "Noun gender"], "relatedWords": ["classification", "noun", "grammar"], "grammarPattern": "gender + agreement/form", "weekIntroduced": 16},
        {"word": "number", "definition": "grammatical feature of quantity", "difficulty": 4, "category": "GRAMMAR_FOCUSED", "contexts": ["Singular number", "Plural number"], "relatedWords": ["quantity", "singular", "plural"], "grammarPattern": "number + agreement/form", "weekIntroduced": 14},
        {"word": "person", "definition": "grammatical category referring to participants", "difficulty": 4, "category": "GRAMMAR_FOCUSED", "contexts": ["First person", "Third person"], "relatedWords": ["participant", "speaker", "reference"], "grammarPattern": "person + form/agreement", "weekIntroduced": 15},
        {"word": "agreement", "definition": "grammatical relationship between elements", "difficulty": 5, "category": "GRAMMAR_FOCUSED", "contexts": ["Subject agreement", "Verb agreement"], "relatedWords": ["harmony", "correspondence", "grammar"], "grammarPattern": "agreement + rule/pattern", "weekIntroduced": 17},
        {"word": "concord", "definition": "agreement between grammatical elements", "difficulty": 6, "category": "GRAMMAR_FOCUSED", "contexts": ["Grammatical concord", "Subject concord"], "relatedWords": ["agreement", "harmony", "correspondence"], "grammarPattern": "concord + rule/principle", "weekIntroduced": 22},
        {"word": "ellipsis", "definition": "omission of words from speech", "difficulty": 6, "category": "GRAMMAR_FOCUSED", "contexts": ["Grammatical ellipsis", "Sentence ellipsis"], "relatedWords": ["omission", "deletion", "implied"], "grammarPattern": "ellipsis + in/of", "weekIntroduced": 23},

        # Advanced academic and scientific terms (35 words)
        {"word": "chromatography", "definition": "technique for separating mixtures", "difficulty": 8, "category": "ACADEMIC", "contexts": ["Gas chromatography", "Liquid chromatography"], "relatedWords": ["separation", "analysis", "chemistry"], "grammarPattern": "chromatography + technique/analysis", "weekIntroduced": 28},
        {"word": "spectroscopy", "definition": "study of interaction between matter and radiation", "difficulty": 8, "category": "ACADEMIC", "contexts": ["Mass spectroscopy", "NMR spectroscopy"], "relatedWords": ["analysis", "radiation", "chemistry"], "grammarPattern": "spectroscopy + analysis/technique", "weekIntroduced": 29},
        {"word": "crystallography", "definition": "science of crystal structure", "difficulty": 9, "category": "ACADEMIC", "contexts": ["X-ray crystallography", "Protein crystallography"], "relatedWords": ["crystal", "structure", "analysis"], "grammarPattern": "crystallography + analysis/study", "weekIntroduced": 30},
        {"word": "electrochemistry", "definition": "study of chemical processes involving electricity", "difficulty": 8, "category": "ACADEMIC", "contexts": ["Applied electrochemistry", "Study electrochemistry"], "relatedWords": ["chemistry", "electricity", "reaction"], "grammarPattern": "electrochemistry + study/application", "weekIntroduced": 28},
        {"word": "thermochemistry", "definition": "study of heat effects in chemical reactions", "difficulty": 8, "category": "ACADEMIC", "contexts": ["Applied thermochemistry", "Study thermochemistry"], "relatedWords": ["heat", "chemistry", "energy"], "grammarPattern": "thermochemistry + study/analysis", "weekIntroduced": 29},
        {"word": "photochemistry", "definition": "study of chemical reactions caused by light", "difficulty": 8, "category": "ACADEMIC", "contexts": ["Atmospheric photochemistry", "Study photochemistry"], "relatedWords": ["light", "chemistry", "reaction"], "grammarPattern": "photochemistry + study/process", "weekIntroduced": 29},
        {"word": "radiochemistry", "definition": "chemistry of radioactive elements", "difficulty": 8, "category": "ACADEMIC", "contexts": ["Nuclear radiochemistry", "Study radiochemistry"], "relatedWords": ["radioactive", "chemistry", "nuclear"], "grammarPattern": "radiochemistry + study/analysis", "weekIntroduced": 30},
        {"word": "geochemistry", "definition": "study of chemical composition of earth", "difficulty": 7, "category": "ACADEMIC", "contexts": ["Environmental geochemistry", "Study geochemistry"], "relatedWords": ["earth", "chemistry", "composition"], "grammarPattern": "geochemistry + study/analysis", "weekIntroduced": 26},
        {"word": "cosmochemistry", "definition": "study of chemical composition of universe", "difficulty": 8, "category": "ACADEMIC", "contexts": ["Planetary cosmochemistry", "Study cosmochemistry"], "relatedWords": ["universe", "chemistry", "space"], "grammarPattern": "cosmochemistry + study/analysis", "weekIntroduced": 29},
        {"word": "astrochemistry", "definition": "study of molecules in space", "difficulty": 8, "category": "ACADEMIC", "contexts": ["Interstellar astrochemistry", "Study astrochemistry"], "relatedWords": ["space", "molecules", "astronomy"], "grammarPattern": "astrochemistry + study/research", "weekIntroduced": 30},
        {"word": "neurochemistry", "definition": "study of chemicals in nervous system", "difficulty": 8, "category": "ACADEMIC", "contexts": ["Brain neurochemistry", "Study neurochemistry"], "relatedWords": ["brain", "chemistry", "nervous"], "grammarPattern": "neurochemistry + study/research", "weekIntroduced": 28},
        {"word": "psychopharmacology", "definition": "study of drug effects on mind", "difficulty": 9, "category": "ACADEMIC", "contexts": ["Clinical psychopharmacology", "Study psychopharmacology"], "relatedWords": ["drugs", "mind", "psychology"], "grammarPattern": "psychopharmacology + study/research", "weekIntroduced": 30},
        {"word": "psychophysiology", "definition": "study of relationship between psychological and physiological processes", "difficulty": 9, "category": "ACADEMIC", "contexts": ["Clinical psychophysiology", "Study psychophysiology"], "relatedWords": ["psychology", "physiology", "brain"], "grammarPattern": "psychophysiology + study/research", "weekIntroduced": 30},
        {"word": "neurophysiology", "definition": "study of function of nervous system", "difficulty": 8, "category": "ACADEMIC", "contexts": ["Cellular neurophysiology", "Study neurophysiology"], "relatedWords": ["nervous", "function", "brain"], "grammarPattern": "neurophysiology + study/research", "weekIntroduced": 29},
        {"word": "electrophysiology", "definition": "study of electrical properties of biological cells", "difficulty": 8, "category": "ACADEMIC", "contexts": ["Cardiac electrophysiology", "Study electrophysiology"], "relatedWords": ["electrical", "cells", "biology"], "grammarPattern": "electrophysiology + study/technique", "weekIntroduced": 29},
        {"word": "psycholinguistics", "definition": "study of psychological aspects of language", "difficulty": 8, "category": "ACADEMIC", "contexts": ["Developmental psycholinguistics", "Study psycholinguistics"], "relatedWords": ["psychology", "language", "mind"], "grammarPattern": "psycholinguistics + study/research", "weekIntroduced": 28},
        {"word": "neurolinguistics", "definition": "study of neural mechanisms of language", "difficulty": 8, "category": "ACADEMIC", "contexts": ["Clinical neurolinguistics", "Study neurolinguistics"], "relatedWords": ["neural", "language", "brain"], "grammarPattern": "neurolinguistics + study/research", "weekIntroduced": 29},
        {"word": "computational", "definition": "using computer calculation", "difficulty": 6, "category": "ACADEMIC", "contexts": ["Computational biology", "Computational linguistics"], "relatedWords": ["computer", "calculation", "digital"], "grammarPattern": "computational + method/approach", "weekIntroduced": 22},
        {"word": "algorithmic", "definition": "relating to algorithms", "difficulty": 7, "category": "ACADEMIC", "contexts": ["Algorithmic approach", "Algorithmic thinking"], "relatedWords": ["algorithm", "systematic", "computational"], "grammarPattern": "algorithmic + approach/method", "weekIntroduced": 25},
        {"word": "heuristic", "definition": "enabling discovery through trial and error", "difficulty": 7, "category": "ACADEMIC", "contexts": ["Heuristic method", "Heuristic approach"], "relatedWords": ["discovery", "learning", "exploratory"], "grammarPattern": "heuristic + method/approach", "weekIntroduced": 26},
        {"word": "stochastic", "definition": "randomly determined", "difficulty": 8, "category": "ACADEMIC", "contexts": ["Stochastic process", "Stochastic model"], "relatedWords": ["random", "probability", "statistical"], "grammarPattern": "stochastic + process/model", "weekIntroduced": 28},
        {"word": "deterministic", "definition": "precisely determined by prior events", "difficulty": 7, "category": "ACADEMIC", "contexts": ["Deterministic system", "Deterministic model"], "relatedWords": ["predictable", "causal", "systematic"], "grammarPattern": "deterministic + system/approach", "weekIntroduced": 26},
        {"word": "probabilistic", "definition": "based on probability", "difficulty": 7, "category": "ACADEMIC", "contexts": ["Probabilistic model", "Probabilistic reasoning"], "relatedWords": ["probability", "likelihood", "uncertain"], "grammarPattern": "probabilistic + model/approach", "weekIntroduced": 25},
        {"word": "statistical", "definition": "relating to statistics", "difficulty": 5, "category": "ACADEMIC", "contexts": ["Statistical analysis", "Statistical significance"], "relatedWords": ["statistics", "data", "numerical"], "grammarPattern": "statistical + analysis/method", "weekIntroduced": 19},
        {"word": "experimental", "definition": "based on experiment", "difficulty": 5, "category": "ACADEMIC", "contexts": ["Experimental design", "Experimental method"], "relatedWords": ["experiment", "testing", "empirical"], "grammarPattern": "experimental + design/approach", "weekIntroduced": 18},
        {"word": "observational", "definition": "based on observation", "difficulty": 6, "category": "ACADEMIC", "contexts": ["Observational study", "Observational data"], "relatedWords": ["observation", "watching", "empirical"], "grammarPattern": "observational + study/method", "weekIntroduced": 21},
        {"word": "longitudinal", "definition": "extending over long period", "difficulty": 7, "category": "ACADEMIC", "contexts": ["Longitudinal study", "Longitudinal analysis"], "relatedWords": ["long-term", "extended", "temporal"], "grammarPattern": "longitudinal + study/analysis", "weekIntroduced": 24},
        {"word": "cross-sectional", "definition": "analyzing at single point in time", "difficulty": 7, "category": "ACADEMIC", "contexts": ["Cross-sectional study", "Cross-sectional analysis"], "relatedWords": ["snapshot", "single-time", "comparative"], "grammarPattern": "cross-sectional + study/analysis", "weekIntroduced": 25},
        {"word": "meta-analysis", "definition": "analysis combining results of multiple studies", "difficulty": 7, "category": "ACADEMIC", "contexts": ["Systematic meta-analysis", "Conduct meta-analysis"], "relatedWords": ["analysis", "combination", "research"], "grammarPattern": "meta-analysis + of/study", "weekIntroduced": 26},
        {"word": "systematic", "definition": "done according to fixed plan", "difficulty": 5, "category": "ACADEMIC", "contexts": ["Systematic review", "Systematic approach"], "relatedWords": ["organized", "methodical", "planned"], "grammarPattern": "systematic + review/approach", "weekIntroduced": 18},
        {"word": "randomized", "definition": "made random in order", "difficulty": 6, "category": "ACADEMIC", "contexts": ["Randomized trial", "Randomized sample"], "relatedWords": ["random", "arbitrary", "unbiased"], "grammarPattern": "randomized + trial/study", "weekIntroduced": 22},
        {"word": "controlled", "definition": "having controls for comparison", "difficulty": 5, "category": "ACADEMIC", "contexts": ["Controlled experiment", "Controlled trial"], "relatedWords": ["regulated", "managed", "supervised"], "grammarPattern": "controlled + experiment/study", "weekIntroduced": 19},
        {"word": "placebo-controlled", "definition": "using inactive treatment for comparison", "difficulty": 7, "category": "ACADEMIC", "contexts": ["Placebo-controlled trial", "Placebo-controlled study"], "relatedWords": ["placebo", "comparison", "control"], "grammarPattern": "placebo-controlled + trial/study", "weekIntroduced": 25},
        {"word": "double-blind", "definition": "neither subject nor researcher knows treatment", "difficulty": 7, "category": "ACADEMIC", "contexts": ["Double-blind study", "Double-blind trial"], "relatedWords": ["blind", "unbiased", "objective"], "grammarPattern": "double-blind + study/trial", "weekIntroduced": 24},
        {"word": "single-blind", "definition": "subject doesn't know which treatment received", "difficulty": 6, "category": "ACADEMIC", "contexts": ["Single-blind study", "Single-blind trial"], "relatedWords": ["blind", "masked", "unbiased"], "grammarPattern": "single-blind + study/trial", "weekIntroduced": 23},

        # Final business and professional terms (35 words)
        {"word": "entrepreneurial", "definition": "relating to entrepreneurship", "difficulty": 6, "category": "BUSINESS", "contexts": ["Entrepreneurial spirit", "Entrepreneurial venture"], "relatedWords": ["innovative", "business", "startup"], "grammarPattern": "entrepreneurial + spirit/venture", "weekIntroduced": 22},
        {"word": "intrapreneurial", "definition": "entrepreneurial within large organization", "difficulty": 7, "category": "BUSINESS", "contexts": ["Intrapreneurial culture", "Intrapreneurial project"], "relatedWords": ["internal", "innovation", "corporate"], "grammarPattern": "intrapreneurial + culture/approach", "weekIntroduced": 26},
        {"word": "managerial", "definition": "relating to management", "difficulty": 5, "category": "BUSINESS", "contexts": ["Managerial skills", "Managerial position"], "relatedWords": ["management", "leadership", "administrative"], "grammarPattern": "managerial + skills/role", "weekIntroduced": 18},
        {"word": "administrative", "definition": "relating to administration", "difficulty": 5, "category": "BUSINESS", "contexts": ["Administrative duties", "Administrative support"], "relatedWords": ["management", "clerical", "organizational"], "grammarPattern": "administrative + duties/support", "weekIntroduced": 17},
        {"word": "operational", "definition": "relating to operations", "difficulty": 5, "category": "BUSINESS", "contexts": ["Operational efficiency", "Operational costs"], "relatedWords": ["functional", "working", "practical"], "grammarPattern": "operational + efficiency/cost", "weekIntroduced": 19},
        {"word": "strategic", "definition": "relating to strategy", "difficulty": 5, "category": "BUSINESS", "contexts": ["Strategic planning", "Strategic decision"], "relatedWords": ["planned", "long-term", "important"], "grammarPattern": "strategic + planning/decision", "weekIntroduced": 18},
        {"word": "tactical", "definition": "relating to tactics", "difficulty": 6, "category": "BUSINESS", "contexts": ["Tactical approach", "Tactical decision"], "relatedWords": ["practical", "immediate", "specific"], "grammarPattern": "tactical + approach/move", "weekIntroduced": 21},
        {"word": "hierarchical", "definition": "arranged in hierarchy", "difficulty": 6, "category": "BUSINESS", "contexts": ["Hierarchical structure", "Hierarchical organization"], "relatedWords": ["ranked", "layered", "ordered"], "grammarPattern": "hierarchical + structure/system", "weekIntroduced": 22},
        {"word": "bureaucratic", "definition": "relating to bureaucracy", "difficulty": 6, "category": "BUSINESS", "contexts": ["Bureaucratic process", "Bureaucratic structure"], "relatedWords": ["administrative", "formal", "complex"], "grammarPattern": "bureaucratic + process/system", "weekIntroduced": 23},
        {"word": "democratic", "definition": "relating to democracy", "difficulty": 5, "category": "BUSINESS", "contexts": ["Democratic leadership", "Democratic decision-making"], "relatedWords": ["participatory", "inclusive", "equal"], "grammarPattern": "democratic + leadership/process", "weekIntroduced": 19},
        {"word": "autocratic", "definition": "having absolute power", "difficulty": 6, "category": "BUSINESS", "contexts": ["Autocratic leadership", "Autocratic management"], "relatedWords": ["authoritarian", "dictatorial", "controlling"], "grammarPattern": "autocratic + leadership/style", "weekIntroduced": 22},
        {"word": "collaborative", "definition": "involving collaboration", "difficulty": 5, "category": "BUSINESS", "contexts": ["Collaborative approach", "Collaborative project"], "relatedWords": ["cooperative", "joint", "shared"], "grammarPattern": "collaborative + approach/effort", "weekIntroduced": 18},
        {"word": "competitive", "definition": "involving competition", "difficulty": 5, "category": "BUSINESS", "contexts": ["Competitive advantage", "Competitive market"], "relatedWords": ["rival", "contest", "struggle"], "grammarPattern": "competitive + advantage/market", "weekIntroduced": 17},
        {"word": "cooperative", "definition": "involving cooperation", "difficulty": 5, "category": "BUSINESS", "contexts": ["Cooperative effort", "Cooperative agreement"], "relatedWords": ["collaborative", "joint", "shared"], "grammarPattern": "cooperative + effort/agreement", "weekIntroduced": 18},
        {"word": "consultative", "definition": "involving consultation", "difficulty": 6, "category": "BUSINESS", "contexts": ["Consultative approach", "Consultative process"], "relatedWords": ["advisory", "guidance", "expert"], "grammarPattern": "consultative + approach/process", "weekIntroduced": 21},
        {"word": "participatory", "definition": "involving participation", "difficulty": 6, "category": "BUSINESS", "contexts": ["Participatory management", "Participatory decision"], "relatedWords": ["inclusive", "involving", "democratic"], "grammarPattern": "participatory + management/approach", "weekIntroduced": 22},
        {"word": "transformational", "definition": "causing transformation", "difficulty": 7, "category": "BUSINESS", "contexts": ["Transformational leadership", "Transformational change"], "relatedWords": ["changing", "revolutionary", "innovative"], "grammarPattern": "transformational + leadership/change", "weekIntroduced": 25},
        {"word": "transactional", "definition": "relating to transactions", "difficulty": 6, "category": "BUSINESS", "contexts": ["Transactional leadership", "Transactional relationship"], "relatedWords": ["exchange", "business", "practical"], "grammarPattern": "transactional + leadership/approach", "weekIntroduced": 23},
        {"word": "motivational", "definition": "providing motivation", "difficulty": 5, "category": "BUSINESS", "contexts": ["Motivational speaker", "Motivational factors"], "relatedWords": ["inspiring", "encouraging", "driving"], "grammarPattern": "motivational + speaker/factor", "weekIntroduced": 19},
        {"word": "organizational", "definition": "relating to organization", "difficulty": 5, "category": "BUSINESS", "contexts": ["Organizational culture", "Organizational structure"], "relatedWords": ["structural", "institutional", "corporate"], "grammarPattern": "organizational + culture/structure", "weekIntroduced": 18},
        {"word": "institutional", "definition": "relating to institution", "difficulty": 6, "category": "BUSINESS", "contexts": ["Institutional policy", "Institutional change"], "relatedWords": ["organizational", "formal", "established"], "grammarPattern": "institutional + policy/change", "weekIntroduced": 21},
        {"word": "multinational", "definition": "operating in several countries", "difficulty": 6, "category": "BUSINESS", "contexts": ["Multinational corporation", "Multinational company"], "relatedWords": ["international", "global", "worldwide"], "grammarPattern": "multinational + corporation/company", "weekIntroduced": 22},
        {"word": "international", "definition": "between nations", "difficulty": 5, "category": "BUSINESS", "contexts": ["International business", "International trade"], "relatedWords": ["global", "worldwide", "foreign"], "grammarPattern": "international + business/trade", "weekIntroduced": 17},
        {"word": "transnational", "definition": "extending beyond national boundaries", "difficulty": 7, "category": "BUSINESS", "contexts": ["Transnational corporation", "Transnational operation"], "relatedWords": ["global", "international", "cross-border"], "grammarPattern": "transnational + corporation/operation", "weekIntroduced": 25},
        {"word": "cross-cultural", "definition": "involving different cultures", "difficulty": 6, "category": "BUSINESS", "contexts": ["Cross-cultural communication", "Cross-cultural management"], "relatedWords": ["multicultural", "diverse", "international"], "grammarPattern": "cross-cultural + communication/management", "weekIntroduced": 23},
        {"word": "intercultural", "definition": "between cultures", "difficulty": 6, "category": "BUSINESS", "contexts": ["Intercultural competence", "Intercultural dialogue"], "relatedWords": ["cross-cultural", "multicultural", "diverse"], "grammarPattern": "intercultural + competence/communication", "weekIntroduced": 24},
        {"word": "multicultural", "definition": "relating to several cultures", "difficulty": 6, "category": "BUSINESS", "contexts": ["Multicultural team", "Multicultural environment"], "relatedWords": ["diverse", "varied", "inclusive"], "grammarPattern": "multicultural + team/environment", "weekIntroduced": 21},
        {"word": "technological", "definition": "relating to technology", "difficulty": 5, "category": "BUSINESS", "contexts": ["Technological advancement", "Technological innovation"], "relatedWords": ["technical", "digital", "modern"], "grammarPattern": "technological + advancement/change", "weekIntroduced": 18},
        {"word": "digital", "definition": "relating to computer technology", "difficulty": 4, "category": "BUSINESS", "contexts": ["Digital transformation", "Digital marketing"], "relatedWords": ["electronic", "computerized", "online"], "grammarPattern": "digital + transformation/technology", "weekIntroduced": 16},
        {"word": "virtual", "definition": "existing in computer simulation", "difficulty": 5, "category": "BUSINESS", "contexts": ["Virtual meeting", "Virtual reality"], "relatedWords": ["simulated", "online", "digital"], "grammarPattern": "virtual + meeting/environment", "weekIntroduced": 19},
        {"word": "automated", "definition": "operated by machines", "difficulty": 5, "category": "BUSINESS", "contexts": ["Automated process", "Automated system"], "relatedWords": ["mechanical", "computerized", "self-operating"], "grammarPattern": "automated + process/system", "weekIntroduced": 20},
        {"word": "mechanized", "definition": "equipped with machinery", "difficulty": 6, "category": "BUSINESS", "contexts": ["Mechanized production", "Mechanized agriculture"], "relatedWords": ["automated", "machine-operated", "industrial"], "grammarPattern": "mechanized + production/process", "weekIntroduced": 23},
        {"word": "industrialized", "definition": "having developed industries", "difficulty": 6, "category": "BUSINESS", "contexts": ["Industrialized country", "Industrialized economy"], "relatedWords": ["developed", "manufacturing", "modern"], "grammarPattern": "industrialized + country/economy", "weekIntroduced": 22},
        {"word": "commercialized", "definition": "managed for profit", "difficulty": 6, "category": "BUSINESS", "contexts": ["Commercialized product", "Commercialized service"], "relatedWords": ["profit-oriented", "marketed", "business"], "grammarPattern": "commercialized + product/service", "weekIntroduced": 24},
        {"word": "privatized", "definition": "transferred to private ownership", "difficulty": 6, "category": "BUSINESS", "contexts": ["Privatized company", "Privatized service"], "relatedWords": ["private", "non-government", "commercial"], "grammarPattern": "privatized + company/industry", "weekIntroduced": 25}
    ]

def main():
    vocab_data = load_vocabulary()
    current_count = len(vocab_data)
    print(f"Current vocabulary: {current_count} words")

    # Add final 100 words
    new_words = add_final_100_words()

    # Filter out duplicates
    existing_words = {item["word"].lower() for item in vocab_data}
    unique_new_words = [word for word in new_words if word["word"].lower() not in existing_words]

    vocab_data.extend(unique_new_words)

    # Save the updated database
    save_vocabulary(vocab_data)

    # Print statistics
    final_count = len(vocab_data)
    added_count = len(unique_new_words)
    print(f"Added {added_count} new words")
    print(f"Total vocabulary: {final_count} words")

    # Category distribution
    from collections import Counter
    categories = Counter(item["category"] for item in vocab_data)
    print(f"Final category distribution: {dict(categories)}")

    if final_count >= 2000:
        print("\n🎉🎉🎉 MISSION ACCOMPLISHED! 🎉🎉🎉")
        print(f"🎯 TARGET EXCEEDED: {final_count} words!")
        print("📚 Comprehensive vocabulary database for YDS/YÖKDİL exam preparation is complete!")
        print("\n📊 Final Statistics:")
        print(f"   • Total words: {final_count}")
        print(f"   • Academic: {categories['ACADEMIC']} words")
        print(f"   • Business: {categories['BUSINESS']} words")
        print(f"   • Exam-specific: {categories['EXAM_SPECIFIC']} words")
        print(f"   • Everyday: {categories['EVERYDAY']} words")
        print(f"   • Grammar-focused: {categories['GRAMMAR_FOCUSED']} words")
        print("\n✅ Ready for Phase 1 implementation!")
    else:
        remaining = 2000 - final_count
        print(f"Still need {remaining} more words to reach 2000")

if __name__ == "__main__":
    main()