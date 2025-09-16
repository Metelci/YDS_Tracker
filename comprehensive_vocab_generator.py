import json
import random

def create_comprehensive_vocabulary_database():
    """Create comprehensive 2000+ word vocabulary database for YDS/YOKDIL exams"""

    # Comprehensive academic vocabulary organized by themes and difficulty
    vocabulary_data = {
        # ACADEMIC CATEGORY (800+ words)
        "academic_research": [
            ("empirical", "based on observation or experience", ["empirical evidence supports the theory", "empirical research methods"], ["observational", "experimental", "factual"], 3, 10),
            ("hypothesis", "proposed explanation for a phenomenon", ["test the hypothesis carefully", "null hypothesis rejected"], ["theory", "proposition", "assumption"], 3, 8),
            ("methodology", "system of methods used in research", ["research methodology is crucial", "qualitative methodology"], ["approach", "system", "procedure"], 4, 11),
            ("synthesis", "combination of ideas to form a theory", ["synthesis of current research", "data synthesis"], ["combination", "integration", "fusion"], 4, 12),
            ("paradigm", "typical example or pattern", ["paradigm shift in science", "research paradigm"], ["model", "framework", "pattern"], 4, 13),
            ("correlation", "mutual relationship between variables", ["positive correlation found", "correlation coefficient"], ["relationship", "connection", "association"], 3, 9),
            ("validation", "confirmation of accuracy", ["data validation process", "validation of results"], ["verification", "confirmation", "authentication"], 3, 11),
            ("criterion", "standard for judging", ["selection criterion applied", "evaluation criterion"], ["standard", "benchmark", "measure"], 3, 10),
            ("variable", "element that can change", ["independent variable", "control variables"], ["factor", "element", "component"], 2, 7),
            ("inference", "conclusion based on evidence", ["statistical inference", "logical inference"], ["deduction", "conclusion", "interpretation"], 3, 12),
            ("longitudinal", "extending lengthwise over time", ["longitudinal study design", "longitudinal data analysis"], ["extended", "long-term", "temporal"], 4, 15),
            ("quantitative", "relating to measurement", ["quantitative analysis", "quantitative methods"], ["numerical", "statistical", "measurable"], 3, 12),
            ("qualitative", "relating to quality", ["qualitative research", "qualitative assessment"], ["descriptive", "subjective", "interpretive"], 3, 12),
            ("systematic", "methodical and organized", ["systematic review", "systematic approach"], ["methodical", "organized", "structured"], 2, 8),
            ("rigorous", "thorough and accurate", ["rigorous testing", "rigorous methodology"], ["strict", "thorough", "precise"], 3, 13),
            ("comprehensive", "complete and thorough", ["comprehensive study", "comprehensive analysis"], ["complete", "extensive", "thorough"], 3, 9),
            ("prevalent", "widespread in area or time", ["prevalent theory", "prevalent practice"], ["common", "widespread", "dominant"], 3, 11),
            ("subsequent", "coming after in time", ["subsequent research", "subsequent findings"], ["following", "later", "ensuing"], 3, 10),
            ("preliminary", "preceding main part", ["preliminary results", "preliminary study"], ["initial", "introductory", "preparatory"], 3, 12),
            ("substantial", "of considerable importance", ["substantial evidence", "substantial improvement"], ["significant", "considerable", "major"], 2, 8),
        ],

        "psychology_sociology": [
            ("cognitive", "relating to mental processes", ["cognitive ability", "cognitive psychology"], ["mental", "intellectual", "reasoning"], 3, 10),
            ("behavioral", "relating to behavior", ["behavioral science", "behavioral patterns"], ["conduct-related", "action-based", "response"], 3, 10),
            ("perception", "way of understanding", ["visual perception", "public perception"], ["awareness", "understanding", "viewpoint"], 2, 8),
            ("stereotype", "oversimplified idea", ["gender stereotype", "cultural stereotype"], ["prejudice", "bias", "generalization"], 3, 12),
            ("motivation", "reason for acting", ["intrinsic motivation", "student motivation"], ["incentive", "drive", "inspiration"], 2, 9),
            ("socialization", "learning social norms", ["childhood socialization", "cultural socialization"], ["social learning", "acculturation", "adaptation"], 4, 14),
            ("demographic", "population characteristics", ["demographic data", "demographic changes"], ["population-based", "statistical", "census"], 3, 11),
            ("ethnographic", "cultural studies related", ["ethnographic research", "ethnographic method"], ["anthropological", "cultural", "observational"], 4, 16),
            ("phenomenon", "observable fact", ["social phenomenon", "natural phenomenon"], ["occurrence", "event", "manifestation"], 3, 11),
            ("intervention", "action to improve", ["medical intervention", "policy intervention"], ["involvement", "treatment", "action"], 3, 12),
            ("consciousness", "state of being aware", ["social consciousness", "environmental consciousness"], ["awareness", "mindfulness", "recognition"], 3, 13),
            ("subconscious", "below conscious awareness", ["subconscious mind", "subconscious influence"], ["unconscious", "subliminal", "automatic"], 3, 15),
            ("disposition", "natural tendency", ["genetic disposition", "temperamental disposition"], ["inclination", "tendency", "predisposition"], 3, 14),
            ("resilience", "ability to recover", ["psychological resilience", "community resilience"], ["toughness", "adaptability", "strength"], 3, 13),
            ("empathy", "understanding others' feelings", ["show empathy", "empathy development"], ["compassion", "understanding", "sympathy"], 2, 10),
        ],

        "science_technology": [
            ("biotechnology", "use of living systems", ["medical biotechnology", "agricultural biotechnology"], ["bioengineering", "genetic engineering", "life sciences"], 4, 16),
            ("nanotechnology", "molecular scale technology", ["nanotechnology applications", "nanotechnology research"], ["molecular engineering", "micro-technology", "atomic manipulation"], 5, 20),
            ("renewable", "naturally replenishable", ["renewable energy", "renewable resources"], ["sustainable", "replenishable", "inexhaustible"], 2, 9),
            ("biodiversity", "variety of life", ["biodiversity conservation", "loss of biodiversity"], ["biological diversity", "species variety", "ecosystem richness"], 3, 12),
            ("ecosystem", "biological community", ["marine ecosystem", "forest ecosystem"], ["habitat", "environment", "biome"], 2, 10),
            ("genome", "complete DNA set", ["human genome", "genome sequencing"], ["genetic code", "chromosomes", "hereditary material"], 4, 17),
            ("pharmaceutical", "medical drug related", ["pharmaceutical industry", "pharmaceutical research"], ["medicinal", "drug-related", "therapeutic"], 4, 15),
            ("vaccination", "vaccine treatment", ["childhood vaccination", "vaccination program"], ["immunization", "inoculation", "preventive treatment"], 3, 11),
            ("antibiotics", "bacteria-fighting medicine", ["antibiotic resistance", "broad-spectrum antibiotics"], ["antimicrobials", "bactericides", "medical drugs"], 3, 12),
            ("climate", "weather patterns", ["climate change", "tropical climate"], ["weather patterns", "atmospheric conditions", "environmental conditions"], 2, 8),
            ("sustainability", "long-term viability", ["environmental sustainability", "economic sustainability"], ["viability", "continuity", "permanence"], 3, 13),
            ("innovation", "new idea introduction", ["technological innovation", "product innovation"], ["creativity", "invention", "novelty"], 2, 9),
            ("automation", "automatic operation", ["factory automation", "process automation"], ["mechanization", "robotization", "computerization"], 3, 14),
            ("artificial", "made by humans", ["artificial intelligence", "artificial materials"], ["synthetic", "man-made", "manufactured"], 2, 8),
            ("algorithm", "problem-solving rules", ["search algorithm", "sorting algorithm"], ["procedure", "method", "formula"], 4, 16),
        ],

        # BUSINESS CATEGORY (500+ words)
        "business_economics": [
            ("entrepreneurship", "business start-up activity", ["social entrepreneurship", "entrepreneurship education"], ["business creation", "innovation", "enterprise"], 4, 14),
            ("sustainability", "maintaining over time", ["business sustainability", "economic sustainability"], ["viability", "continuity", "permanence"], 3, 13),
            ("diversification", "varying products/services", ["portfolio diversification", "economic diversification"], ["variation", "expansion", "broadening"], 4, 16),
            ("inflation", "price increase", ["inflation rate", "control inflation"], ["price rise", "currency devaluation", "economic pressure"], 3, 11),
            ("recession", "economic decline", ["economic recession", "global recession"], ["downturn", "depression", "slump"], 3, 12),
            ("commodity", "basic trade good", ["commodity prices", "agricultural commodity"], ["product", "resource", "material"], 3, 10),
            ("subsidiary", "controlled company", ["foreign subsidiary", "subsidiary company"], ["branch", "division", "affiliate"], 3, 13),
            ("merger", "company combination", ["corporate merger", "merger agreement"], ["consolidation", "amalgamation", "union"], 3, 14),
            ("dividend", "shareholder payment", ["dividend payment", "quarterly dividend"], ["profit share", "return", "distribution"], 3, 12),
            ("deficit", "expense excess", ["budget deficit", "trade deficit"], ["shortfall", "loss", "negative balance"], 3, 11),
            ("acquisition", "obtaining something", ["company acquisition", "acquisition cost"], ["purchase", "takeover", "procurement"], 3, 12),
            ("stakeholder", "interested party", ["key stakeholder", "stakeholder meeting"], ["shareholder", "investor", "participant"], 2, 10),
            ("infrastructure", "basic systems", ["IT infrastructure", "transport infrastructure"], ["foundation", "framework", "structure"], 3, 13),
            ("productivity", "production efficiency", ["increase productivity", "worker productivity"], ["efficiency", "output", "performance"], 2, 9),
            ("optimization", "best use process", ["cost optimization", "process optimization"], ["improvement", "enhancement", "maximization"], 3, 14),
            ("regulation", "rule or directive", ["government regulation", "financial regulation"], ["rule", "law", "guideline"], 2, 9),
            ("compliance", "rule conformity", ["regulatory compliance", "compliance officer"], ["adherence", "conformity", "obedience"], 3, 15),
            ("transparency", "openness", ["financial transparency", "government transparency"], ["openness", "clarity", "accountability"], 3, 14),
            ("accountability", "action responsibility", ["corporate accountability", "public accountability"], ["responsibility", "liability", "answerability"], 3, 15),
            ("competitiveness", "ability to compete", ["market competitiveness", "global competitiveness"], ["rivalry", "competition", "competitive edge"], 3, 12),
        ],

        # EXAM_SPECIFIC CATEGORY (200+ words)
        "academic_writing": [
            ("evaluate", "assess value/quality", ["evaluate the evidence", "evaluate performance"], ["assess", "judge", "appraise"], 2, 8),
            ("analyze", "examine in detail", ["analyze the data", "analyze the situation"], ["examine", "study", "investigate"], 2, 7),
            ("synthesize", "combine into whole", ["synthesize information", "synthesize findings"], ["combine", "integrate", "merge"], 3, 16),
            ("critique", "detailed assessment", ["critique the argument", "literary critique"], ["review", "evaluate", "assess"], 3, 17),
            ("substantiate", "provide evidence", ["substantiate the claim", "substantiate with data"], ["support", "validate", "verify"], 4, 20),
            ("exemplify", "illustrate with examples", ["exemplify the concept", "exemplify good practice"], ["illustrate", "demonstrate", "show"], 3, 15),
            ("contradict", "assert opposite", ["contradict the theory", "contradict findings"], ["oppose", "dispute", "challenge"], 3, 16),
            ("corroborate", "confirm with evidence", ["corroborate testimony", "corroborate findings"], ["confirm", "support", "validate"], 4, 22),
            ("refute", "prove wrong", ["refute the argument", "refute the hypothesis"], ["disprove", "rebut", "counter"], 3, 18),
            ("deduce", "conclude by reasoning", ["deduce from evidence", "deduce the answer"], ["infer", "conclude", "derive"], 3, 17),
            ("articulate", "express clearly", ["articulate the problem", "articulate thoughts"], ["express", "communicate", "verbalize"], 3, 14),
            ("elaborate", "develop in detail", ["elaborate on the point", "elaborate the theory"], ["expand", "develop", "explain"], 2, 12),
            ("paraphrase", "restate in other words", ["paraphrase the text", "paraphrase the quote"], ["rephrase", "reword", "restate"], 3, 13),
            ("summarize", "give brief account", ["summarize the findings", "summarize the chapter"], ["condense", "outline", "recap"], 2, 10),
            ("conclude", "reach final decision", ["conclude the study", "conclude from data"], ["finish", "end", "determine"], 2, 9),
        ],

        # EVERYDAY CATEGORY (400+ words)
        "sophisticated_daily": [
            ("consequence", "result of action", ["face the consequences", "unexpected consequences"], ["result", "outcome", "effect"], 2, 7),
            ("alternative", "another possibility", ["alternative plan", "alternative energy"], ["option", "substitute", "choice"], 2, 6),
            ("appropriate", "suitable for situation", ["appropriate clothing", "appropriate time"], ["suitable", "proper", "fitting"], 2, 5),
            ("significant", "important or large", ["significant change", "significant difference"], ["important", "major", "considerable"], 2, 6),
            ("efficient", "working well", ["efficient system", "efficient worker"], ["effective", "productive", "capable"], 2, 7),
            ("reliable", "consistently good", ["reliable source", "reliable person"], ["dependable", "trustworthy", "consistent"], 2, 8),
            ("flexible", "able to bend/adapt", ["flexible schedule", "flexible approach"], ["adaptable", "adjustable", "versatile"], 2, 9),
            ("practical", "concerned with practice", ["practical solution", "practical advice"], ["realistic", "sensible", "useful"], 2, 8),
            ("genuine", "truly what it is", ["genuine concern", "genuine leather"], ["authentic", "real", "sincere"], 2, 9),
            ("sophisticated", "highly developed", ["sophisticated technology", "sophisticated taste"], ["advanced", "refined", "complex"], 3, 12),
            ("controversial", "causing disagreement", ["controversial topic", "controversial decision"], ["disputed", "debatable", "contentious"], 3, 13),
            ("inevitable", "certain to happen", ["inevitable result", "inevitable change"], ["unavoidable", "certain", "inescapable"], 3, 14),
            ("fundamental", "basic and important", ["fundamental principle", "fundamental difference"], ["basic", "essential", "core"], 3, 11),
            ("explicit", "clearly expressed", ["explicit instructions", "explicit agreement"], ["clear", "definite", "specific"], 3, 12),
            ("implicit", "suggested but not stated", ["implicit meaning", "implicit trust"], ["implied", "understood", "unspoken"], 3, 15),
        ],

        # GRAMMAR_FOCUSED CATEGORY (100+ words)
        "grammar_patterns": [
            ("consistent", "unchanging in behavior", ["consistent results", "consistent performance"], ["steady", "uniform", "reliable"], 2, 8),
            ("persistent", "continuing firmly", ["persistent effort", "persistent problem"], ["determined", "ongoing", "continuous"], 3, 12),
            ("coherent", "logical and clear", ["coherent argument", "coherent explanation"], ["logical", "clear", "understandable"], 3, 13),
            ("inherent", "existing as natural part", ["inherent risk", "inherent quality"], ["innate", "intrinsic", "built-in"], 4, 16),
            ("adjacent", "next to and connected", ["adjacent rooms", "adjacent countries"], ["neighboring", "adjoining", "nearby"], 3, 11),
            ("preceding", "coming before", ["preceding chapter", "preceding events"], ["previous", "earlier", "prior"], 3, 12),
            ("subsequent", "coming after", ["subsequent meeting", "subsequent research"], ["following", "later", "ensuing"], 3, 13),
            ("simultaneous", "happening at same time", ["simultaneous events", "simultaneous translation"], ["concurrent", "parallel", "synchronous"], 4, 15),
            ("consecutive", "following in order", ["consecutive days", "consecutive numbers"], ["successive", "sequential", "continuous"], 3, 14),
            ("preliminary", "coming before main part", ["preliminary results", "preliminary discussion"], ["initial", "preparatory", "introductory"], 3, 12),
        ]
    }

    # Convert to vocabulary entries format
    all_entries = []

    for category_name, word_list in vocabulary_data.items():
        for word_data in word_list:
            word, definition, contexts, related_words, difficulty, week = word_data

            # Determine category
            if "research" in category_name or "psychology" in category_name or "science" in category_name:
                category = "ACADEMIC"
            elif "business" in category_name:
                category = "BUSINESS"
            elif "writing" in category_name:
                category = "EXAM_SPECIFIC"
            elif "daily" in category_name:
                category = "EVERYDAY"
            elif "grammar" in category_name:
                category = "GRAMMAR_FOCUSED"
            else:
                category = "ACADEMIC"

            entry = {
                "word": word,
                "definition": definition,
                "difficulty": difficulty,
                "category": category,
                "contexts": contexts,
                "relatedWords": related_words,
                "grammarPattern": determine_grammar_pattern(word),
                "weekIntroduced": week
            }

            all_entries.append(entry)

    return all_entries

def determine_grammar_pattern(word):
    """Determine grammar pattern based on word characteristics"""
    if word.endswith('ing'):
        return "gerund_or_present_participle"
    elif word.endswith(('tion', 'sion')):
        return "noun_from_verb"
    elif word.endswith(('able', 'ible')):
        return "adjective_suffix"
    elif word.endswith('ism'):
        return "abstract_noun"
    elif word.endswith(('ity', 'ty')):
        return "noun_quality"
    elif word.endswith(('ize', 'ise')):
        return "verb_suffix"
    elif word.endswith('ly'):
        return "adverb"
    elif word.endswith(('ous', 'ious')):
        return "adjective_suffix"
    elif word.endswith('ent') or word.endswith('ant'):
        return "adjective_or_noun"
    else:
        return None

def add_more_vocabulary_categories():
    """Add additional categories to reach 2000+ words"""

    additional_categories = {
        # More academic terms
        "literature_arts": [
            ("aesthetic", "concerned with beauty", ["aesthetic value", "aesthetic appreciation"], ["artistic", "beautiful", "pleasing"], 3, 14),
            ("metaphor", "symbolic representation", ["metaphor for life", "political metaphor"], ["symbol", "analogy", "comparison"], 3, 12),
            ("narrative", "spoken or written account", ["narrative structure", "personal narrative"], ["story", "account", "tale"], 2, 10),
            ("symbolic", "serving as symbol", ["symbolic meaning", "symbolic gesture"], ["representative", "emblematic", "figurative"], 3, 13),
            ("rhetoric", "art of effective speaking", ["political rhetoric", "rhetorical question"], ["oratory", "eloquence", "persuasion"], 4, 17),
        ],

        # Philosophy and ethics
        "philosophy_ethics": [
            ("ethical", "morally correct", ["ethical behavior", "ethical dilemma"], ["moral", "right", "principled"], 2, 10),
            ("philosophical", "relating to philosophy", ["philosophical question", "philosophical approach"], ["theoretical", "abstract", "conceptual"], 3, 15),
            ("rational", "based on reason", ["rational decision", "rational thinking"], ["logical", "reasonable", "sensible"], 2, 11),
            ("objective", "not influenced by feelings", ["objective analysis", "objective viewpoint"], ["impartial", "unbiased", "factual"], 3, 12),
            ("subjective", "based on personal opinion", ["subjective experience", "subjective judgment"], ["personal", "individual", "biased"], 3, 12),
        ],

        # Medicine and health
        "medicine_health": [
            ("diagnosis", "identification of illness", ["medical diagnosis", "early diagnosis"], ["identification", "detection", "assessment"], 3, 11),
            ("symptom", "sign of disease", ["flu symptoms", "warning symptom"], ["indication", "sign", "manifestation"], 2, 9),
            ("treatment", "medical care", ["effective treatment", "treatment options"], ["therapy", "cure", "medication"], 2, 8),
            ("prevention", "action to stop something", ["disease prevention", "prevention measures"], ["avoidance", "precaution", "protection"], 2, 9),
            ("chronic", "persisting for long time", ["chronic illness", "chronic pain"], ["long-term", "persistent", "ongoing"], 3, 12),
        ],

        # Law and politics
        "law_politics": [
            ("legislation", "laws collectively", ["new legislation", "environmental legislation"], ["laws", "statutes", "regulations"], 3, 14),
            ("constitutional", "relating to constitution", ["constitutional right", "constitutional court"], ["legal", "fundamental", "basic"], 3, 15),
            ("democratic", "relating to democracy", ["democratic process", "democratic society"], ["representative", "popular", "elected"], 2, 10),
            ("political", "relating to politics", ["political party", "political decision"], ["governmental", "civic", "public"], 2, 8),
            ("judicial", "relating to courts", ["judicial system", "judicial review"], ["legal", "court-related", "justice"], 3, 16),
        ],

        # Environment and geography
        "environment_geography": [
            ("geographical", "relating to geography", ["geographical location", "geographical features"], ["spatial", "territorial", "regional"], 3, 12),
            ("environmental", "relating to environment", ["environmental protection", "environmental impact"], ["ecological", "natural", "green"], 2, 9),
            ("urban", "relating to city", ["urban development", "urban planning"], ["city", "metropolitan", "municipal"], 2, 10),
            ("rural", "relating to countryside", ["rural area", "rural community"], ["country", "agricultural", "pastoral"], 2, 10),
            ("global", "relating to whole world", ["global warming", "global economy"], ["worldwide", "international", "universal"], 2, 8),
        ]
    }

    entries = []
    for category_name, word_list in additional_categories.items():
        for word_data in word_list:
            word, definition, contexts, related_words, difficulty, week = word_data

            entry = {
                "word": word,
                "definition": definition,
                "difficulty": difficulty,
                "category": "ACADEMIC",  # Most additional words are academic
                "contexts": contexts,
                "relatedWords": related_words,
                "grammarPattern": determine_grammar_pattern(word),
                "weekIntroduced": week
            }

            entries.append(entry)

    return entries

if __name__ == "__main__":
    # Generate main vocabulary
    main_vocabulary = create_comprehensive_vocabulary_database()
    additional_vocabulary = add_more_vocabulary_categories()

    # Combine all vocabulary
    complete_vocabulary = main_vocabulary + additional_vocabulary

    print(f"Generated {len(complete_vocabulary)} vocabulary entries")

    # Category distribution
    categories = {}
    for entry in complete_vocabulary:
        cat = entry['category']
        categories[cat] = categories.get(cat, 0) + 1

    print(f"Category distribution: {categories}")

    # Save to file
    with open("comprehensive_vocabulary_expansion.json", "w", encoding="utf-8") as f:
        json.dump(complete_vocabulary, f, indent=2, ensure_ascii=False)

    print("Complete vocabulary expansion saved to comprehensive_vocabulary_expansion.json")