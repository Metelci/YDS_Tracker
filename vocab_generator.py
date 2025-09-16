import json
import random

# Academic vocabulary for YDS/YOKDIL exams - systematic generation
def generate_academic_vocabulary():
    """Generate comprehensive academic vocabulary database"""

    # Academic vocabulary by category and difficulty
    academic_words = {
        # Level 2-3 Academic Terms (Weeks 10-15)
        "research_methodology": [
            ("empirical", "based on observation or experience", ["empirical evidence", "empirical research"], ["observational", "experimental", "factual"]),
            ("hypothesis", "proposed explanation for a phenomenon", ["test the hypothesis", "null hypothesis"], ["theory", "proposition", "assumption"]),
            ("methodology", "system of methods used in research", ["research methodology", "teaching methodology"], ["approach", "system", "procedure"]),
            ("synthesis", "combination of ideas to form a theory", ["synthesis of research", "chemical synthesis"], ["combination", "integration", "fusion"]),
            ("paradigm", "typical example or pattern", ["paradigm shift", "research paradigm"], ["model", "framework", "pattern"]),
            ("correlation", "mutual relationship between variables", ["positive correlation", "statistical correlation"], ["relationship", "connection", "association"]),
            ("validation", "confirmation of accuracy or truth", ["data validation", "validation process"], ["confirmation", "verification", "authentication"]),
            ("criterion", "standard for judging", ["selection criterion", "evaluation criterion"], ["standard", "benchmark", "measure"]),
            ("variable", "element that can change", ["independent variable", "control variable"], ["factor", "element", "component"]),
            ("inference", "conclusion reached on evidence", ["statistical inference", "logical inference"], ["deduction", "conclusion", "interpretation"])
        ],

        "psychology_sociology": [
            ("cognitive", "relating to mental processes", ["cognitive ability", "cognitive psychology"], ["mental", "intellectual", "reasoning"]),
            ("behavioral", "relating to behavior", ["behavioral science", "behavioral patterns"], ["conduct-related", "action-based", "response"]),
            ("perception", "way of understanding something", ["visual perception", "public perception"], ["awareness", "understanding", "viewpoint"]),
            ("stereotype", "oversimplified idea about a group", ["gender stereotype", "cultural stereotype"], ["prejudice", "bias", "generalization"]),
            ("motivation", "reason for acting", ["intrinsic motivation", "student motivation"], ["incentive", "drive", "inspiration"]),
            ("socialization", "process of learning social norms", ["childhood socialization", "cultural socialization"], ["social learning", "acculturation", "adaptation"]),
            ("demographic", "relating to population characteristics", ["demographic data", "demographic changes"], ["population-based", "statistical", "census-related"]),
            ("ethnographic", "relating to cultural studies", ["ethnographic research", "ethnographic method"], ["anthropological", "cultural", "observational"]),
            ("phenomenon", "observable fact or event", ["social phenomenon", "natural phenomenon"], ["occurrence", "event", "manifestation"]),
            ("intervention", "action taken to improve situation", ["medical intervention", "policy intervention"], ["involvement", "treatment", "action"])
        ],

        "economics_business": [
            ("entrepreneurship", "activity of starting businesses", ["social entrepreneurship", "entrepreneurship education"], ["business creation", "innovation", "enterprise"]),
            ("sustainability", "ability to maintain over time", ["environmental sustainability", "economic sustainability"], ["viability", "continuity", "permanence"]),
            ("diversification", "process of varying products", ["portfolio diversification", "economic diversification"], ["variation", "expansion", "broadening"]),
            ("inflation", "general increase in prices", ["inflation rate", "control inflation"], ["price rise", "currency devaluation", "economic pressure"]),
            ("recession", "economic decline period", ["economic recession", "global recession"], ["downturn", "depression", "slump"]),
            ("commodity", "basic good in commerce", ["commodity prices", "agricultural commodity"], ["product", "resource", "material"]),
            ("subsidiary", "company controlled by another", ["foreign subsidiary", "subsidiary company"], ["branch", "division", "affiliate"]),
            ("merger", "combination of companies", ["corporate merger", "merger agreement"], ["consolidation", "amalgamation", "union"]),
            ("dividend", "payment to shareholders", ["dividend payment", "quarterly dividend"], ["profit share", "return", "distribution"]),
            ("deficit", "amount by which expenses exceed income", ["budget deficit", "trade deficit"], ["shortfall", "loss", "negative balance"])
        ],

        "science_technology": [
            ("biotechnology", "use of living systems for technology", ["medical biotechnology", "agricultural biotechnology"], ["bioengineering", "genetic engineering", "life sciences"]),
            ("nanotechnology", "technology on molecular scale", ["nanotechnology applications", "nanotechnology research"], ["molecular engineering", "micro-technology", "atomic manipulation"]),
            ("renewable", "able to be renewed naturally", ["renewable energy", "renewable resources"], ["sustainable", "replenishable", "inexhaustible"]),
            ("biodiversity", "variety of life in ecosystems", ["biodiversity conservation", "loss of biodiversity"], ["biological diversity", "species variety", "ecosystem richness"]),
            ("ecosystem", "biological community and environment", ["marine ecosystem", "forest ecosystem"], ["habitat", "environment", "biome"]),
            ("genome", "complete set of DNA", ["human genome", "genome sequencing"], ["genetic code", "chromosomes", "hereditary material"]),
            ("pharmaceutical", "relating to medical drugs", ["pharmaceutical industry", "pharmaceutical research"], ["medicinal", "drug-related", "therapeutic"]),
            ("vaccination", "treatment with vaccine", ["childhood vaccination", "vaccination program"], ["immunization", "inoculation", "preventive treatment"]),
            ("antibiotics", "medicines that fight bacteria", ["antibiotic resistance", "broad-spectrum antibiotics"], ["antimicrobials", "bactericides", "medical drugs"]),
            ("climate", "long-term weather patterns", ["climate change", "tropical climate"], ["weather patterns", "atmospheric conditions", "environmental conditions"])
        ]
    }

    # Generate JSON entries
    vocabulary_entries = []
    week_counter = 10

    for category, word_groups in academic_words.items():
        for word, definition, contexts, related_words in word_groups:

            # Determine category mapping
            if "research" in category or "science" in category:
                vocab_category = "ACADEMIC"
            elif "economics" in category or "business" in category:
                vocab_category = "BUSINESS"
            elif "psychology" in category:
                vocab_category = "ACADEMIC"
            else:
                vocab_category = "ACADEMIC"

            # Determine difficulty based on word complexity
            difficulty = 3 if len(word) > 8 or word.endswith(('tion', 'ism', 'ity')) else 2

            entry = {
                "word": word,
                "definition": definition,
                "difficulty": difficulty,
                "category": vocab_category,
                "contexts": contexts,
                "relatedWords": related_words,
                "grammarPattern": determine_grammar_pattern(word),
                "weekIntroduced": week_counter
            }

            vocabulary_entries.append(entry)

            # Increment week every 10 words
            if len(vocabulary_entries) % 10 == 0:
                week_counter += 1
                if week_counter > 30:
                    week_counter = 30

    return vocabulary_entries

def determine_grammar_pattern(word):
    """Determine grammar pattern based on word characteristics"""
    if word.endswith('ing'):
        return "gerund_or_present_participle"
    elif word.endswith('tion') or word.endswith('sion'):
        return "noun_from_verb"
    elif word.endswith('able') or word.endswith('ible'):
        return "adjective_suffix"
    elif word.endswith('ism'):
        return "abstract_noun"
    elif word.endswith('ity') or word.endswith('ty'):
        return "noun_quality"
    elif word.endswith('ize') or word.endswith('ise'):
        return "verb_suffix"
    elif word.endswith('ly'):
        return "adverb"
    else:
        return None

# Generate additional vocabulary categories
def generate_business_vocabulary():
    """Generate business and professional vocabulary"""
    business_terms = [
        ("acquisition", "act of acquiring something", ["company acquisition", "acquisition cost"], ["purchase", "takeover", "procurement"], 3, "BUSINESS", 12),
        ("stakeholder", "person with interest in enterprise", ["key stakeholder", "stakeholder meeting"], ["shareholder", "investor", "participant"], 2, "BUSINESS", 11),
        ("infrastructure", "basic physical systems", ["IT infrastructure", "transport infrastructure"], ["foundation", "framework", "structure"], 3, "BUSINESS", 13),
        ("productivity", "efficiency in production", ["increase productivity", "worker productivity"], ["efficiency", "output", "performance"], 2, "BUSINESS", 12),
        ("optimization", "process of making best use", ["cost optimization", "process optimization"], ["improvement", "enhancement", "maximization"], 3, "BUSINESS", 14),
        ("regulation", "rule or directive", ["government regulation", "financial regulation"], ["rule", "law", "guideline"], 2, "BUSINESS", 11),
        ("compliance", "conformity with rules", ["regulatory compliance", "compliance officer"], ["adherence", "conformity", "obedience"], 3, "BUSINESS", 15),
        ("transparency", "openness and honesty", ["financial transparency", "government transparency"], ["openness", "clarity", "accountability"], 3, "BUSINESS", 14),
        ("accountability", "responsibility for actions", ["corporate accountability", "public accountability"], ["responsibility", "liability", "answerability"], 3, "BUSINESS", 15),
        ("innovation", "introduction of new ideas", ["technological innovation", "product innovation"], ["creativity", "invention", "novelty"], 2, "BUSINESS", 12)
    ]

    vocabulary_entries = []
    for word, definition, contexts, related_words, difficulty, category, week in business_terms:
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
        vocabulary_entries.append(entry)

    return vocabulary_entries

def generate_exam_specific_vocabulary():
    """Generate exam and academic writing specific vocabulary"""
    exam_terms = [
        ("evaluate", "assess the value or quality", ["evaluate the evidence", "evaluate student performance"], ["assess", "judge", "appraise"], 2, "EXAM_SPECIFIC", 8),
        ("analyze", "examine in detail", ["analyze the data", "analyze the situation"], ["examine", "study", "investigate"], 2, "EXAM_SPECIFIC", 7),
        ("synthesize", "combine elements into whole", ["synthesize information", "synthesize research findings"], ["combine", "integrate", "merge"], 3, "EXAM_SPECIFIC", 16),
        ("critique", "detailed analysis and assessment", ["critique the argument", "literary critique"], ["review", "evaluate", "assess"], 3, "EXAM_SPECIFIC", 17),
        ("substantiate", "provide evidence to support", ["substantiate the claim", "substantiate with data"], ["support", "validate", "verify"], 4, "EXAM_SPECIFIC", 20),
        ("exemplify", "illustrate with examples", ["exemplify the concept", "exemplify good practice"], ["illustrate", "demonstrate", "show"], 3, "EXAM_SPECIFIC", 15),
        ("contradict", "assert the opposite", ["contradict the theory", "contradict previous findings"], ["oppose", "dispute", "challenge"], 3, "EXAM_SPECIFIC", 16),
        ("corroborate", "confirm with evidence", ["corroborate the testimony", "corroborate findings"], ["confirm", "support", "validate"], 4, "EXAM_SPECIFIC", 22),
        ("refute", "prove statement wrong", ["refute the argument", "refute the hypothesis"], ["disprove", "rebut", "counter"], 3, "EXAM_SPECIFIC", 18),
        ("deduce", "arrive at conclusion by reasoning", ["deduce from evidence", "deduce the answer"], ["infer", "conclude", "derive"], 3, "EXAM_SPECIFIC", 17)
    ]

    vocabulary_entries = []
    for word, definition, contexts, related_words, difficulty, category, week in exam_terms:
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
        vocabulary_entries.append(entry)

    return vocabulary_entries

# Generate and combine all vocabulary
def create_comprehensive_database():
    """Create the complete vocabulary database"""
    all_vocabulary = []

    # Add different categories
    all_vocabulary.extend(generate_academic_vocabulary())
    all_vocabulary.extend(generate_business_vocabulary())
    all_vocabulary.extend(generate_exam_specific_vocabulary())

    return all_vocabulary

if __name__ == "__main__":
    # Generate vocabulary
    new_vocabulary = create_comprehensive_database()

    print(f"Generated {len(new_vocabulary)} new vocabulary entries")

    # Save as JSON
    with open("vocabulary_expansion_batch1.json", "w", encoding="utf-8") as f:
        json.dump(new_vocabulary, f, indent=2, ensure_ascii=False)

    print("Vocabulary expansion saved to vocabulary_expansion_batch1.json")
    print(f"Sample entry: {new_vocabulary[0] if new_vocabulary else 'None'}")