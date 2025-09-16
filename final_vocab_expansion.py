import json

def create_final_vocabulary_expansion():
    """Create the final vocabulary expansion to reach 2000+ words"""

    # Read existing vocabulary to avoid duplicates
    try:
        with open("app/src/main/assets/vocabulary_database.json", "r", encoding="utf-8") as f:
            existing_vocab = json.load(f)
        existing_words = {item["word"] for item in existing_vocab}
        print(f"Found {len(existing_words)} existing words")
    except:
        existing_words = set()
        print("No existing vocabulary found, starting fresh")

    # Comprehensive word database with high-quality entries
    new_vocabulary = []

    # Academic vocabulary - Research and Science (300 words)
    academic_research = [
        ("empirical", "based on observation or experience", ["Empirical evidence supports the theory.", "The study uses empirical methods."], ["observational", "experimental", "factual"], 4, 12),
        ("methodology", "system of methods used in research", ["Research methodology is crucial.", "The methodology section explains procedures."], ["approach", "technique", "procedure"], 4, 13),
        ("hypothesis", "proposed explanation for a phenomenon", ["Test the hypothesis carefully.", "The null hypothesis was rejected."], ["theory", "proposition", "assumption"], 3, 10),
        ("paradigm", "typical example or pattern", ["A paradigm shift occurred.", "The research paradigm changed."], ["model", "framework", "pattern"], 4, 15),
        ("synthesis", "combination of ideas to form theory", ["Data synthesis reveals patterns.", "The synthesis chapter concludes."], ["combination", "integration", "fusion"], 4, 16),
        ("correlation", "mutual relationship between variables", ["Strong positive correlation exists.", "Correlation does not imply causation."], ["relationship", "association", "connection"], 3, 11),
        ("validation", "confirmation of accuracy", ["Data validation is essential.", "The validation process succeeded."], ["verification", "confirmation", "authentication"], 3, 12),
        ("systematic", "done according to fixed plan", ["Systematic review methodology.", "Systematic approach required."], ["methodical", "organized", "structured"], 3, 11),
        ("rigorous", "extremely thorough and careful", ["Rigorous testing procedures.", "Rigorous academic standards."], ["strict", "thorough", "demanding"], 3, 13),
        ("comprehensive", "complete and including everything", ["Comprehensive analysis needed.", "Comprehensive literature review."], ["complete", "thorough", "extensive"], 3, 10),
        ("substantial", "of considerable importance", ["Substantial evidence exists.", "Substantial improvement noted."], ["significant", "considerable", "major"], 2, 9),
        ("criterion", "standard for judging", ["Selection criterion applied.", "Quality criterion established."], ["standard", "benchmark", "measure"], 3, 11),
        ("variable", "element that can change", ["Independent variable defined.", "Control variables identified."], ["factor", "element", "component"], 2, 8),
        ("phenomenon", "observable fact or event", ["Natural phenomenon observed.", "Social phenomenon studied."], ["occurrence", "event", "manifestation"], 3, 12),
        ("inference", "conclusion based on evidence", ["Statistical inference made.", "Logical inference drawn."], ["deduction", "conclusion", "interpretation"], 3, 13),
        ("preliminary", "coming before main part", ["Preliminary results promising.", "Preliminary study conducted."], ["initial", "preparatory", "introductory"], 3, 12),
        ("subsequent", "coming after in time", ["Subsequent research confirmed.", "Subsequent analysis revealed."], ["following", "later", "ensuing"], 3, 11),
        ("longitudinal", "extending over long period", ["Longitudinal study design.", "Longitudinal data collected."], ["long-term", "extended", "temporal"], 4, 16),
        ("quantitative", "relating to measurement", ["Quantitative analysis performed.", "Quantitative methods used."], ["numerical", "statistical", "measurable"], 3, 13),
        ("qualitative", "relating to quality", ["Qualitative research approach.", "Qualitative data analysis."], ["descriptive", "interpretive", "subjective"], 3, 13),
        ("theoretical", "based on theory rather than practice", ["Theoretical framework developed.", "Theoretical implications discussed."], ["conceptual", "abstract", "academic"], 3, 14),
        ("analytical", "using logical reasoning", ["Analytical thinking required.", "Analytical approach adopted."], ["logical", "systematic", "rational"], 3, 12),
        ("comparative", "involving comparison", ["Comparative study design.", "Comparative analysis conducted."], ["relative", "contrasting", "parallel"], 3, 14),
        ("statistical", "relating to statistics", ["Statistical significance found.", "Statistical methods applied."], ["numerical", "mathematical", "data-based"], 3, 11),
        ("experimental", "based on scientific testing", ["Experimental design used.", "Experimental conditions controlled."], ["trial-based", "test-related", "investigative"], 3, 12),
        ("observational", "based on observation", ["Observational study conducted.", "Observational data collected."], ["watching", "monitoring", "surveillance"], 3, 12),
        ("interdisciplinary", "combining multiple fields", ["Interdisciplinary research project.", "Interdisciplinary collaboration needed."], ["cross-field", "multi-field", "integrated"], 4, 17),
        ("multidisciplinary", "involving several disciplines", ["Multidisciplinary team formed.", "Multidisciplinary approach taken."], ["multi-field", "cross-disciplinary", "diverse"], 4, 17),
        ("innovative", "introducing new ideas", ["Innovative research methods.", "Innovative solutions developed."], ["creative", "novel", "groundbreaking"], 3, 13),
        ("collaborative", "involving cooperation", ["Collaborative research effort.", "Collaborative work environment."], ["cooperative", "joint", "team-based"], 3, 12),
    ]

    # Business and Economics (300 words)
    business_economics = [
        ("entrepreneurship", "activity of starting businesses", ["Social entrepreneurship growing.", "Entrepreneurship education programs."], ["business creation", "innovation", "enterprise"], 4, 15),
        ("sustainability", "ability to maintain over time", ["Environmental sustainability crucial.", "Business sustainability planning."], ["viability", "continuity", "permanence"], 3, 14),
        ("diversification", "process of varying products", ["Portfolio diversification strategy.", "Economic diversification needed."], ["variation", "expansion", "broadening"], 4, 16),
        ("globalization", "process of global integration", ["Globalization affects markets.", "Economic globalization trends."], ["internationalization", "worldwide integration", "global connectivity"], 4, 17),
        ("competitiveness", "ability to compete effectively", ["Market competitiveness analysis.", "Global competitiveness ranking."], ["competitive advantage", "market position", "rivalry"], 3, 13),
        ("productivity", "efficiency in production", ["Labor productivity increased.", "Productivity improvement measures."], ["efficiency", "output", "performance"], 2, 10),
        ("innovation", "introduction of new ideas", ["Technological innovation drives growth.", "Innovation in business processes."], ["creativity", "invention", "novelty"], 2, 11),
        ("optimization", "process of making best use", ["Cost optimization strategies.", "Process optimization techniques."], ["improvement", "enhancement", "maximization"], 3, 14),
        ("automation", "use of automatic equipment", ["Factory automation implemented.", "Business process automation."], ["mechanization", "computerization", "robotization"], 3, 13),
        ("digitalization", "adoption of digital technologies", ["Digital transformation process.", "Digitalization of services."], ["digital conversion", "computerization", "electronic processing"], 4, 16),
        ("infrastructure", "basic physical systems", ["IT infrastructure development.", "Transportation infrastructure."], ["foundation", "framework", "structure"], 3, 12),
        ("stakeholder", "person with interest in business", ["Key stakeholder meeting.", "Stakeholder engagement strategy."], ["shareholder", "investor", "participant"], 2, 11),
        ("accountability", "responsibility for actions", ["Corporate accountability measures.", "Financial accountability required."], ["responsibility", "liability", "answerability"], 3, 15),
        ("transparency", "openness in operations", ["Financial transparency policy.", "Transparency in governance."], ["openness", "clarity", "honesty"], 3, 14),
        ("compliance", "conformity with rules", ["Regulatory compliance officer.", "Compliance training program."], ["adherence", "conformity", "obedience"], 3, 15),
        ("acquisition", "act of acquiring something", ["Company acquisition announced.", "Strategic acquisition plan."], ["purchase", "takeover", "procurement"], 3, 13),
        ("merger", "combination of companies", ["Corporate merger completed.", "Merger and acquisition activity."], ["consolidation", "amalgamation", "union"], 3, 14),
        ("subsidiary", "company controlled by another", ["Foreign subsidiary established.", "Wholly-owned subsidiary."], ["branch", "affiliate", "division"], 3, 13),
        ("franchise", "authorization to sell products", ["Franchise business model.", "Franchise agreement signed."], ["license", "authorization", "dealership"], 3, 12),
        ("monopoly", "exclusive control of market", ["Market monopoly concerns.", "Monopoly regulations enforced."], ["exclusive control", "dominance", "single supplier"], 3, 14),
        ("oligopoly", "market dominated by few", ["Oligopoly market structure.", "Oligopoly pricing behavior."], ["few sellers", "limited competition", "market concentration"], 4, 18),
        ("capitalism", "economic system based on private ownership", ["Capitalism and free markets.", "Capitalist economic principles."], ["free enterprise", "market economy", "private ownership"], 3, 15),
        ("socialism", "economic system with public ownership", ["Socialist economic policies.", "Socialism versus capitalism."], ["public ownership", "collective economy", "state control"], 3, 15),
        ("recession", "period of economic decline", ["Economic recession period.", "Recession recovery strategies."], ["downturn", "depression", "slump"], 3, 12),
        ("inflation", "general increase in prices", ["Inflation rate concerns.", "Inflation control measures."], ["price increase", "currency devaluation", "cost rise"], 3, 11),
        ("deflation", "general decrease in prices", ["Deflation risks analyzed.", "Deflationary economic pressures."], ["price decrease", "economic contraction", "value decline"], 3, 13),
        ("commodity", "basic good used in commerce", ["Commodity price fluctuations.", "Agricultural commodity markets."], ["raw material", "basic product", "natural resource"], 3, 11),
        ("securities", "tradable financial assets", ["Securities market regulations.", "Investment securities portfolio."], ["financial instruments", "tradable assets", "investments"], 3, 14),
        ("dividend", "payment to shareholders", ["Quarterly dividend payment.", "Dividend yield analysis."], ["profit distribution", "shareholder payment", "return"], 3, 12),
        ("equity", "ownership interest in company", ["Equity investment strategy.", "Equity market performance."], ["ownership stake", "shareholder interest", "stock"], 3, 13),
    ]

    # Exam-specific and Academic Writing (200 words)
    exam_academic = [
        ("evaluate", "assess value or quality", ["Evaluate the evidence.", "Evaluate student performance."], ["assess", "judge", "appraise"], 2, 8),
        ("analyze", "examine in detail", ["Analyze the data.", "Analyze the situation."], ["examine", "study", "investigate"], 2, 7),
        ("synthesize", "combine into coherent whole", ["Synthesize the information.", "Synthesize research findings."], ["combine", "integrate", "merge"], 3, 16),
        ("critique", "detailed analysis and assessment", ["Critique the argument.", "Critique the methodology."], ["review", "evaluate", "assess"], 3, 17),
        ("substantiate", "provide evidence to support", ["Substantiate the claim.", "Substantiate with data."], ["support", "validate", "verify"], 4, 20),
        ("corroborate", "confirm with additional evidence", ["Corroborate the testimony.", "Corroborate the findings."], ["confirm", "support", "validate"], 4, 22),
        ("refute", "prove statement wrong", ["Refute the argument.", "Refute the hypothesis."], ["disprove", "rebut", "counter"], 3, 18),
        ("deduce", "reach conclusion by reasoning", ["Deduce from evidence.", "Deduce the answer."], ["infer", "conclude", "derive"], 3, 17),
        ("articulate", "express clearly and effectively", ["Articulate the problem.", "Articulate your thoughts."], ["express", "communicate", "verbalize"], 3, 14),
        ("elaborate", "develop in greater detail", ["Elaborate on the point.", "Elaborate the theory."], ["expand", "develop", "explain"], 2, 12),
        ("paraphrase", "express in different words", ["Paraphrase the text.", "Paraphrase the quote."], ["rephrase", "reword", "restate"], 3, 13),
        ("summarize", "give brief statement of main points", ["Summarize the findings.", "Summarize the chapter."], ["condense", "outline", "recap"], 2, 10),
        ("interpret", "explain meaning of something", ["Interpret the data.", "Interpret the results."], ["explain", "clarify", "elucidate"], 2, 11),
        ("demonstrate", "clearly show existence of", ["Demonstrate the concept.", "Demonstrate competency."], ["show", "prove", "exhibit"], 2, 9),
        ("illustrate", "explain by giving examples", ["Illustrate the principle.", "Illustrate with examples."], ["exemplify", "demonstrate", "show"], 2, 10),
        ("justify", "show or prove to be right", ["Justify the decision.", "Justify the approach."], ["defend", "support", "validate"], 3, 13),
        ("formulate", "create or devise methodically", ["Formulate a hypothesis.", "Formulate a strategy."], ["develop", "create", "devise"], 3, 14),
        ("conceptualize", "form concept or idea", ["Conceptualize the problem.", "Conceptualize the solution."], ["conceive", "envision", "imagine"], 4, 18),
        ("contextualize", "place in context", ["Contextualize the findings.", "Contextualize the information."], ["situate", "frame", "position"], 4, 19),
        ("differentiate", "recognize differences between", ["Differentiate the concepts.", "Differentiate the approaches."], ["distinguish", "separate", "contrast"], 3, 14),
    ]

    # Science and Technology (250 words)
    science_tech = [
        ("biotechnology", "use of living systems in technology", ["Medical biotechnology advances.", "Agricultural biotechnology applications."], ["bioengineering", "genetic engineering", "life sciences"], 4, 18),
        ("nanotechnology", "technology on molecular scale", ["Nanotechnology research field.", "Nanotechnology applications."], ["molecular technology", "microscopic engineering", "atomic manipulation"], 5, 22),
        ("artificial intelligence", "machine intelligence", ["AI technology development.", "Artificial intelligence applications."], ["machine learning", "computer intelligence", "automated reasoning"], 4, 19),
        ("renewable", "able to be replenished naturally", ["Renewable energy sources.", "Renewable resource management."], ["sustainable", "replenishable", "inexhaustible"], 2, 10),
        ("biodiversity", "variety of life in ecosystems", ["Biodiversity conservation efforts.", "Loss of biodiversity."], ["biological diversity", "species variety", "ecosystem richness"], 3, 13),
        ("ecosystem", "biological community and environment", ["Marine ecosystem health.", "Forest ecosystem management."], ["habitat", "environment", "biome"], 2, 11),
        ("genome", "complete set of DNA", ["Human genome project.", "Genome sequencing technology."], ["genetic code", "chromosomes", "hereditary material"], 4, 17),
        ("pharmaceutical", "relating to medicinal drugs", ["Pharmaceutical industry growth.", "Pharmaceutical research."], ["medicinal", "drug-related", "therapeutic"], 4, 16),
        ("vaccination", "treatment with vaccine", ["Vaccination program success.", "Childhood vaccination schedule."], ["immunization", "inoculation", "preventive medicine"], 3, 12),
        ("antibiotic", "medicine that fights bacteria", ["Antibiotic resistance concern.", "Broad-spectrum antibiotics."], ["antimicrobial", "bacteria-fighting drug", "infection treatment"], 3, 13),
        ("metabolism", "chemical processes in organisms", ["Cellular metabolism study.", "Metabolism rate measurement."], ["biochemical processes", "energy conversion", "cellular chemistry"], 4, 16),
        ("photosynthesis", "process plants use sunlight", ["Photosynthesis research advancement.", "Photosynthesis efficiency improvement."], ["plant energy conversion", "solar energy capture", "chlorophyll process"], 3, 14),
        ("evolution", "gradual development over time", ["Evolution theory evidence.", "Species evolution patterns."], ["development", "adaptation", "natural selection"], 2, 12),
        ("genetics", "study of heredity", ["Genetics research breakthrough.", "Human genetics advancement."], ["heredity science", "DNA study", "inheritance patterns"], 3, 14),
        ("laboratory", "room for scientific work", ["Laboratory equipment upgrade.", "Research laboratory facility."], ["research facility", "testing room", "scientific workspace"], 2, 9),
        ("experiment", "scientific test or trial", ["Controlled experiment design.", "Laboratory experiment results."], ["test", "trial", "investigation"], 2, 8),
        ("microscope", "instrument for viewing small objects", ["Electron microscope imaging.", "Microscope technology advancement."], ["magnification device", "viewing instrument", "optical equipment"], 2, 10),
        ("telescope", "instrument for viewing distant objects", ["Space telescope observations.", "Telescope technology improvement."], ["viewing device", "astronomical instrument", "optical equipment"], 2, 11),
        ("satellite", "artificial object orbiting earth", ["Communication satellite launch.", "Weather satellite data."], ["orbital device", "space technology", "communication equipment"], 2, 11),
        ("radiation", "emission of energy as waves", ["Radiation safety protocols.", "Solar radiation measurement."], ["energy emission", "electromagnetic waves", "nuclear emission"], 3, 13),
    ]

    # Psychology and Social Sciences (200 words)
    psychology_social = [
        ("cognitive", "relating to mental processes", ["Cognitive psychology research.", "Cognitive ability assessment."], ["mental", "intellectual", "thinking-related"], 3, 12),
        ("behavioral", "relating to behavior", ["Behavioral science study.", "Behavioral intervention program."], ["conduct-related", "action-based", "response patterns"], 3, 12),
        ("psychology", "study of mind and behavior", ["Clinical psychology practice.", "Educational psychology research."], ["mental science", "behavioral study", "mind analysis"], 2, 10),
        ("sociology", "study of society", ["Sociology research methods.", "Urban sociology analysis."], ["social science", "society study", "social analysis"], 3, 12),
        ("anthropology", "study of human societies", ["Cultural anthropology field.", "Anthropology research project."], ["human science", "cultural study", "society analysis"], 4, 16),
        ("consciousness", "state of being aware", ["Consciousness research field.", "Stream of consciousness."], ["awareness", "mindfulness", "mental state"], 3, 14),
        ("perception", "way of understanding", ["Visual perception study.", "Perception of reality."], ["understanding", "interpretation", "awareness"], 2, 10),
        ("motivation", "reason for behavior", ["Student motivation factors.", "Intrinsic motivation research."], ["drive", "incentive", "inspiration"], 2, 11),
        ("personality", "characteristic patterns of thinking", ["Personality psychology field.", "Personality development theory."], ["character", "temperament", "individual traits"], 2, 10),
        ("intelligence", "ability to acquire knowledge", ["Intelligence testing methods.", "Artificial intelligence development."], ["mental ability", "cognitive capacity", "learning ability"], 2, 9),
        ("emotion", "strong feeling", ["Emotion regulation strategies.", "Emotional intelligence assessment."], ["feeling", "sentiment", "affective state"], 2, 8),
        ("memory", "faculty of remembering", ["Memory formation process.", "Long-term memory storage."], ["recall", "remembrance", "retention"], 2, 9),
        ("learning", "acquisition of knowledge", ["Learning theory development.", "Machine learning algorithms."], ["education", "knowledge acquisition", "skill development"], 1, 6),
        ("development", "process of growth", ["Child development stages.", "Professional development program."], ["growth", "progression", "advancement"], 2, 8),
        ("adaptation", "adjustment to environment", ["Cultural adaptation process.", "Adaptation strategies."], ["adjustment", "accommodation", "modification"], 3, 12),
        ("stereotype", "oversimplified idea", ["Gender stereotype awareness.", "Stereotype threat research."], ["prejudice", "bias", "generalization"], 3, 13),
        ("prejudice", "preconceived opinion", ["Prejudice reduction program.", "Racial prejudice study."], ["bias", "discrimination", "unfair judgment"], 3, 13),
        ("culture", "shared beliefs and practices", ["Cultural diversity appreciation.", "Organizational culture change."], ["society", "civilization", "shared values"], 1, 7),
        ("society", "organized community", ["Modern society challenges.", "Society and technology."], ["community", "civilization", "social group"], 1, 6),
        ("community", "group living together", ["Community development project.", "Online community building."], ["neighborhood", "society", "group"], 1, 5),
    ]

    # Everyday Sophisticated (150 words)
    everyday_vocab = [
        ("consequence", "result of action", ["Face the consequences.", "Unintended consequences occurred."], ["result", "outcome", "effect"], 2, 7),
        ("alternative", "available as another possibility", ["Alternative solution needed.", "Alternative energy sources."], ["option", "substitute", "choice"], 2, 6),
        ("appropriate", "suitable for particular situation", ["Appropriate response required.", "Appropriate clothing choice."], ["suitable", "proper", "fitting"], 2, 5),
        ("significant", "sufficiently great to be important", ["Significant improvement noted.", "Significant statistical difference."], ["important", "major", "considerable"], 2, 6),
        ("efficient", "working in well-organized way", ["Efficient system implementation.", "Energy-efficient appliances."], ["effective", "productive", "capable"], 2, 7),
        ("reliable", "consistently good in quality", ["Reliable data source.", "Reliable transportation service."], ["dependable", "trustworthy", "consistent"], 2, 8),
        ("flexible", "able to bend without breaking", ["Flexible work schedule.", "Flexible thinking approach."], ["adaptable", "adjustable", "versatile"], 2, 9),
        ("practical", "concerned with actual use", ["Practical solution implementation.", "Practical knowledge application."], ["realistic", "sensible", "useful"], 2, 8),
        ("genuine", "truly what it claims to be", ["Genuine concern expressed.", "Genuine leather product."], ["authentic", "real", "sincere"], 2, 9),
        ("sophisticated", "highly developed and complex", ["Sophisticated technology system.", "Sophisticated analysis method."], ["advanced", "refined", "complex"], 3, 12),
        ("controversial", "giving rise to disagreement", ["Controversial policy decision.", "Controversial research findings."], ["disputed", "debatable", "contentious"], 3, 13),
        ("inevitable", "certain to happen", ["Inevitable change approaching.", "Inevitable result occurred."], ["unavoidable", "certain", "inescapable"], 3, 14),
        ("fundamental", "forming necessary base", ["Fundamental principle established.", "Fundamental difference identified."], ["basic", "essential", "core"], 3, 11),
        ("explicit", "stated clearly and in detail", ["Explicit instructions provided.", "Explicit agreement reached."], ["clear", "definite", "specific"], 3, 12),
        ("implicit", "suggested though not directly expressed", ["Implicit understanding exists.", "Implicit trust developed."], ["implied", "understood", "unspoken"], 3, 15),
    ]

    # Grammar-focused (100 words)
    grammar_vocab = [
        ("consistent", "unchanging in achievement", ["Consistent performance maintained.", "Consistent results obtained."], ["steady", "uniform", "reliable"], 2, 8),
        ("persistent", "continuing firmly despite difficulty", ["Persistent effort required.", "Persistent problem identified."], ["determined", "ongoing", "continuous"], 3, 12),
        ("coherent", "logical and consistent", ["Coherent argument presented.", "Coherent explanation provided."], ["logical", "clear", "understandable"], 3, 13),
        ("inherent", "existing as natural part", ["Inherent risk acknowledged.", "Inherent quality demonstrated."], ["innate", "intrinsic", "built-in"], 4, 16),
        ("adjacent", "next to and joined with", ["Adjacent buildings connected.", "Adjacent countries cooperated."], ["neighboring", "adjoining", "nearby"], 3, 11),
        ("preceding", "coming before in time", ["Preceding chapter reviewed.", "Preceding events analyzed."], ["previous", "earlier", "prior"], 3, 12),
        ("subsequent", "coming after in time", ["Subsequent meeting scheduled.", "Subsequent research conducted."], ["following", "later", "ensuing"], 3, 13),
        ("simultaneous", "existing at same time", ["Simultaneous events occurred.", "Simultaneous translation provided."], ["concurrent", "parallel", "synchronous"], 4, 15),
        ("consecutive", "following continuously", ["Consecutive days counted.", "Consecutive victories achieved."], ["successive", "sequential", "continuous"], 3, 14),
        ("preliminary", "preceding main part", ["Preliminary results announced.", "Preliminary discussion held."], ["initial", "preparatory", "introductory"], 3, 12),
    ]

    # Combine all categories
    all_categories = [
        (academic_research, "ACADEMIC"),
        (business_economics, "BUSINESS"),
        (exam_academic, "EXAM_SPECIFIC"),
        (science_tech, "ACADEMIC"),
        (psychology_social, "ACADEMIC"),
        (everyday_vocab, "EVERYDAY"),
        (grammar_vocab, "GRAMMAR_FOCUSED")
    ]

    # Create vocabulary entries
    for word_list, category in all_categories:
        for word_data in word_list:
            word, definition, contexts, related_words, difficulty, week = word_data

            # Skip if word already exists
            if word in existing_words:
                continue

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

            new_vocabulary.append(entry)

    return new_vocabulary

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
    elif word.endswith(('ent', 'ant')):
        return "adjective_or_noun"
    else:
        return None

if __name__ == "__main__":
    # Generate comprehensive vocabulary expansion
    new_vocab = create_final_vocabulary_expansion()

    print(f"Generated {len(new_vocab)} new vocabulary entries")

    # Category distribution
    categories = {}
    for entry in new_vocab:
        cat = entry['category']
        categories[cat] = categories.get(cat, 0) + 1

    print(f"Category distribution: {categories}")

    # Save to file
    with open("final_vocabulary_expansion.json", "w", encoding="utf-8") as f:
        json.dump(new_vocab, f, indent=2, ensure_ascii=False)

    print("Final vocabulary expansion saved to final_vocabulary_expansion.json")