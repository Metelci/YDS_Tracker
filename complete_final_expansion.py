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

def add_advanced_academic_terms():
    return [
        # Advanced academic vocabulary for YDS/YÖKDİL
        {"word": "substantiate", "definition": "to provide evidence to support or prove the truth of", "difficulty": 8, "category": "ACADEMIC", "contexts": ["Please substantiate your claims with evidence", "The research substantiates the hypothesis"], "relatedWords": ["validate", "confirm", "verify"], "grammarPattern": "substantiate + claim/argument", "weekIntroduced": 20},
        {"word": "elucidate", "definition": "to make something clear; explain", "difficulty": 9, "category": "ACADEMIC", "contexts": ["The professor elucidated the complex theory", "Could you elucidate your position?"], "relatedWords": ["clarify", "explain", "illuminate"], "grammarPattern": "elucidate + concept/theory", "weekIntroduced": 21},
        {"word": "proliferate", "definition": "to increase rapidly in numbers; multiply", "difficulty": 7, "category": "ACADEMIC", "contexts": ["Social media platforms have proliferated", "Cancer cells proliferate rapidly"], "relatedWords": ["multiply", "spread", "expand"], "grammarPattern": "proliferate + rapidly/quickly", "weekIntroduced": 18},
        {"word": "extrapolate", "definition": "to extend the application of a method or conclusion", "difficulty": 8, "category": "ACADEMIC", "contexts": ["We can extrapolate from these results", "Don't extrapolate too much from limited data"], "relatedWords": ["infer", "deduce", "project"], "grammarPattern": "extrapolate from + data/results", "weekIntroduced": 22},
        {"word": "juxtapose", "definition": "to place or deal with close together for contrasting effect", "difficulty": 8, "category": "ACADEMIC", "contexts": ["The artist juxtaposed modern and classical elements", "Juxtapose these two theories"], "relatedWords": ["contrast", "compare", "oppose"], "grammarPattern": "juxtapose + A with B", "weekIntroduced": 23},
        {"word": "corroborate", "definition": "to confirm or give support to a statement or theory", "difficulty": 8, "category": "ACADEMIC", "contexts": ["The witness corroborated his testimony", "Evidence corroborates the theory"], "relatedWords": ["confirm", "support", "validate"], "grammarPattern": "corroborate + statement/evidence", "weekIntroduced": 21},
        {"word": "amalgamate", "definition": "to combine or unite to form one organization or structure", "difficulty": 7, "category": "ACADEMIC", "contexts": ["The companies decided to amalgamate", "Amalgamate different approaches"], "relatedWords": ["merge", "unite", "combine"], "grammarPattern": "amalgamate with/into", "weekIntroduced": 19},
        {"word": "oscillate", "definition": "to move or swing back and forth in a regular rhythm", "difficulty": 7, "category": "ACADEMIC", "contexts": ["The pendulum oscillates regularly", "Public opinion oscillates between extremes"], "relatedWords": ["fluctuate", "vary", "swing"], "grammarPattern": "oscillate between + A and B", "weekIntroduced": 20},
        {"word": "exemplify", "definition": "to be a typical example of something", "difficulty": 6, "category": "ACADEMIC", "contexts": ["This case exemplifies the problem", "She exemplifies dedication"], "relatedWords": ["illustrate", "demonstrate", "represent"], "grammarPattern": "exemplify + quality/concept", "weekIntroduced": 17},
        {"word": "articulate", "definition": "to express thoughts or feelings clearly", "difficulty": 6, "category": "ACADEMIC", "contexts": ["She articulated her concerns clearly", "Articulate your ideas precisely"], "relatedWords": ["express", "voice", "communicate"], "grammarPattern": "articulate + idea/concern", "weekIntroduced": 16},

        # Advanced exam-specific terms
        {"word": "chronological", "definition": "arranged in the order in which they occurred", "difficulty": 6, "category": "EXAM_SPECIFIC", "contexts": ["Arrange events in chronological order", "A chronological account of history"], "relatedWords": ["sequential", "temporal", "ordered"], "grammarPattern": "chronological order/sequence", "weekIntroduced": 15},
        {"word": "demographic", "definition": "relating to the structure of populations", "difficulty": 7, "category": "EXAM_SPECIFIC", "contexts": ["Demographic changes affect society", "Study demographic data"], "relatedWords": ["population", "statistical", "social"], "grammarPattern": "demographic + data/change", "weekIntroduced": 18},
        {"word": "infrastructure", "definition": "the basic physical systems of a country or organization", "difficulty": 6, "category": "EXAM_SPECIFIC", "contexts": ["Invest in infrastructure development", "Digital infrastructure is crucial"], "relatedWords": ["foundation", "framework", "system"], "grammarPattern": "infrastructure + development/investment", "weekIntroduced": 16},
        {"word": "sustainability", "definition": "the ability to maintain something at a certain rate or level", "difficulty": 7, "category": "EXAM_SPECIFIC", "contexts": ["Environmental sustainability is important", "Economic sustainability concerns"], "relatedWords": ["durability", "continuity", "viability"], "grammarPattern": "sustainability + of/in", "weekIntroduced": 19},
        {"word": "globalization", "definition": "the process of international integration", "difficulty": 7, "category": "EXAM_SPECIFIC", "contexts": ["Globalization affects local cultures", "Economic globalization trends"], "relatedWords": ["internationalization", "integration", "worldwide"], "grammarPattern": "globalization + effects/impact", "weekIntroduced": 20},

        # Advanced everyday sophisticated terms
        {"word": "meticulous", "definition": "showing great attention to detail; very careful", "difficulty": 6, "category": "EVERYDAY", "contexts": ["He's meticulous about his work", "Meticulous planning is required"], "relatedWords": ["careful", "precise", "thorough"], "grammarPattern": "meticulous + about/in", "weekIntroduced": 17},
        {"word": "conscientious", "definition": "diligent, careful, and dutiful", "difficulty": 6, "category": "EVERYDAY", "contexts": ["She's a conscientious student", "Conscientious work habits"], "relatedWords": ["diligent", "responsible", "careful"], "grammarPattern": "conscientious + about/in", "weekIntroduced": 16},
        {"word": "pragmatic", "definition": "practical rather than idealistic", "difficulty": 7, "category": "EVERYDAY", "contexts": ["Take a pragmatic approach", "He's very pragmatic about business"], "relatedWords": ["practical", "realistic", "sensible"], "grammarPattern": "pragmatic approach/solution", "weekIntroduced": 18},
        {"word": "sophisticated", "definition": "having great knowledge or experience", "difficulty": 6, "category": "EVERYDAY", "contexts": ["A sophisticated analysis", "Sophisticated technology"], "relatedWords": ["advanced", "complex", "refined"], "grammarPattern": "sophisticated + system/approach", "weekIntroduced": 15},
        {"word": "innovative", "definition": "featuring new methods; advanced and original", "difficulty": 5, "category": "EVERYDAY", "contexts": ["An innovative solution", "Innovative technology"], "relatedWords": ["creative", "original", "novel"], "grammarPattern": "innovative + approach/solution", "weekIntroduced": 14},

        # More business terms
        {"word": "entrepreneurial", "definition": "relating to entrepreneurship", "difficulty": 6, "category": "BUSINESS", "contexts": ["Entrepreneurial spirit", "Entrepreneurial ventures"], "relatedWords": ["innovative", "business-minded", "enterprising"], "grammarPattern": "entrepreneurial + spirit/venture", "weekIntroduced": 17},
        {"word": "stakeholder", "definition": "a person with an interest in something", "difficulty": 6, "category": "BUSINESS", "contexts": ["Consult all stakeholders", "Stakeholder meeting"], "relatedWords": ["investor", "participant", "party"], "grammarPattern": "stakeholder + engagement/meeting", "weekIntroduced": 16},
        {"word": "subsidiary", "definition": "a company controlled by another company", "difficulty": 7, "category": "BUSINESS", "contexts": ["A subsidiary company", "Establish a subsidiary"], "relatedWords": ["branch", "division", "affiliate"], "grammarPattern": "subsidiary + of/company", "weekIntroduced": 19},
        {"word": "acquisition", "definition": "the act of acquiring something", "difficulty": 6, "category": "BUSINESS", "contexts": ["Company acquisition", "Acquisition strategy"], "relatedWords": ["purchase", "takeover", "merger"], "grammarPattern": "acquisition + of/strategy", "weekIntroduced": 18},
        {"word": "diversification", "definition": "the action of diversifying", "difficulty": 7, "category": "BUSINESS", "contexts": ["Portfolio diversification", "Business diversification"], "relatedWords": ["variety", "expansion", "spread"], "grammarPattern": "diversification + strategy/of", "weekIntroduced": 20}
    ]

def add_comprehensive_academic_vocabulary():
    return [
        # Philosophy and abstract thinking
        {"word": "empirical", "definition": "based on observation or experience", "difficulty": 8, "category": "ACADEMIC", "contexts": ["Empirical evidence supports the theory", "An empirical approach to research"], "relatedWords": ["observational", "experimental", "factual"], "grammarPattern": "empirical + evidence/research", "weekIntroduced": 22},
        {"word": "theoretical", "definition": "concerned with or involving theory", "difficulty": 6, "category": "ACADEMIC", "contexts": ["Theoretical framework", "From a theoretical perspective"], "relatedWords": ["conceptual", "abstract", "hypothetical"], "grammarPattern": "theoretical + framework/approach", "weekIntroduced": 18},
        {"word": "paradigm", "definition": "a typical example or pattern of something", "difficulty": 8, "category": "ACADEMIC", "contexts": ["A new paradigm in science", "Paradigm shift in thinking"], "relatedWords": ["model", "framework", "example"], "grammarPattern": "paradigm + shift/change", "weekIntroduced": 23},
        {"word": "methodology", "definition": "a system of methods used in research", "difficulty": 7, "category": "ACADEMIC", "contexts": ["Research methodology", "Develop a methodology"], "relatedWords": ["approach", "method", "system"], "grammarPattern": "methodology + for/of", "weekIntroduced": 21},
        {"word": "hypothesis", "definition": "a supposition made as a starting point for investigation", "difficulty": 6, "category": "ACADEMIC", "contexts": ["Test the hypothesis", "Form a hypothesis"], "relatedWords": ["theory", "assumption", "proposition"], "grammarPattern": "hypothesis + about/that", "weekIntroduced": 19},

        # Science and research
        {"word": "phenomenon", "definition": "a fact or situation that is observed to exist", "difficulty": 7, "category": "ACADEMIC", "contexts": ["A natural phenomenon", "Study the phenomenon"], "relatedWords": ["occurrence", "event", "manifestation"], "grammarPattern": "phenomenon + of/in", "weekIntroduced": 20},
        {"word": "correlation", "definition": "a mutual relationship between two or more things", "difficulty": 7, "category": "ACADEMIC", "contexts": ["Strong correlation between variables", "Find a correlation"], "relatedWords": ["relationship", "connection", "association"], "grammarPattern": "correlation + between/with", "weekIntroduced": 21},
        {"word": "variable", "definition": "an element that may change within the context", "difficulty": 6, "category": "ACADEMIC", "contexts": ["Control the variables", "Independent variable"], "relatedWords": ["factor", "element", "component"], "grammarPattern": "variable + in/of", "weekIntroduced": 18},
        {"word": "synthesis", "definition": "the combination of components to form a connected whole", "difficulty": 8, "category": "ACADEMIC", "contexts": ["Synthesis of ideas", "Chemical synthesis"], "relatedWords": ["combination", "integration", "merger"], "grammarPattern": "synthesis + of/between", "weekIntroduced": 22},
        {"word": "catalyst", "definition": "a person or thing that precipitates an event", "difficulty": 7, "category": "ACADEMIC", "contexts": ["Act as a catalyst for change", "Chemical catalyst"], "relatedWords": ["trigger", "stimulant", "accelerator"], "grammarPattern": "catalyst + for/of", "weekIntroduced": 21},

        # Advanced descriptive terms
        {"word": "ubiquitous", "definition": "present, appearing, or found everywhere", "difficulty": 8, "category": "ACADEMIC", "contexts": ["Smartphones are ubiquitous", "Ubiquitous computing"], "relatedWords": ["universal", "widespread", "omnipresent"], "grammarPattern": "ubiquitous + in/throughout", "weekIntroduced": 24},
        {"word": "ambiguous", "definition": "open to more than one interpretation", "difficulty": 7, "category": "ACADEMIC", "contexts": ["Ambiguous statement", "The result is ambiguous"], "relatedWords": ["unclear", "vague", "uncertain"], "grammarPattern": "ambiguous + about/regarding", "weekIntroduced": 20},
        {"word": "comprehensive", "definition": "complete and including everything", "difficulty": 6, "category": "ACADEMIC", "contexts": ["Comprehensive study", "Comprehensive coverage"], "relatedWords": ["complete", "thorough", "extensive"], "grammarPattern": "comprehensive + study/analysis", "weekIntroduced": 17},
        {"word": "systematic", "definition": "done according to a fixed plan or system", "difficulty": 6, "category": "ACADEMIC", "contexts": ["Systematic approach", "Systematic review"], "relatedWords": ["organized", "methodical", "structured"], "grammarPattern": "systematic + approach/method", "weekIntroduced": 18},
        {"word": "concurrent", "definition": "existing or happening at the same time", "difficulty": 7, "category": "ACADEMIC", "contexts": ["Concurrent events", "Concurrent processing"], "relatedWords": ["simultaneous", "parallel", "coexisting"], "grammarPattern": "concurrent + with/events", "weekIntroduced": 21},

        # Process and change
        {"word": "transformation", "definition": "a thorough or dramatic change", "difficulty": 6, "category": "ACADEMIC", "contexts": ["Digital transformation", "Personal transformation"], "relatedWords": ["change", "conversion", "metamorphosis"], "grammarPattern": "transformation + of/in", "weekIntroduced": 19},
        {"word": "evolution", "definition": "the gradual development of something", "difficulty": 5, "category": "ACADEMIC", "contexts": ["Evolution of technology", "Social evolution"], "relatedWords": ["development", "progression", "growth"], "grammarPattern": "evolution + of/in", "weekIntroduced": 16},
        {"word": "implementation", "definition": "the process of putting a decision into effect", "difficulty": 6, "category": "ACADEMIC", "contexts": ["Implementation of policies", "Successful implementation"], "relatedWords": ["execution", "application", "realization"], "grammarPattern": "implementation + of/phase", "weekIntroduced": 17},
        {"word": "optimization", "definition": "the action of making the best use of something", "difficulty": 7, "category": "ACADEMIC", "contexts": ["Process optimization", "System optimization"], "relatedWords": ["improvement", "enhancement", "maximization"], "grammarPattern": "optimization + of/for", "weekIntroduced": 20},
        {"word": "integration", "definition": "the action of combining things into a whole", "difficulty": 6, "category": "ACADEMIC", "contexts": ["System integration", "Social integration"], "relatedWords": ["combination", "merger", "unification"], "grammarPattern": "integration + of/with", "weekIntroduced": 18},

        # Analysis and evaluation
        {"word": "criterion", "definition": "a principle or standard by which something is judged", "difficulty": 7, "category": "ACADEMIC", "contexts": ["Selection criterion", "Evaluation criterion"], "relatedWords": ["standard", "measure", "benchmark"], "grammarPattern": "criterion + for/of", "weekIntroduced": 20},
        {"word": "assessment", "definition": "the action of assessing someone or something", "difficulty": 5, "category": "ACADEMIC", "contexts": ["Performance assessment", "Risk assessment"], "relatedWords": ["evaluation", "appraisal", "analysis"], "grammarPattern": "assessment + of/for", "weekIntroduced": 15},
        {"word": "interpretation", "definition": "the action of explaining the meaning of something", "difficulty": 6, "category": "ACADEMIC", "contexts": ["Data interpretation", "Different interpretation"], "relatedWords": ["explanation", "analysis", "understanding"], "grammarPattern": "interpretation + of/for", "weekIntroduced": 17},
        {"word": "evaluation", "definition": "the making of a judgment about something", "difficulty": 5, "category": "ACADEMIC", "contexts": ["Project evaluation", "Critical evaluation"], "relatedWords": ["assessment", "appraisal", "review"], "grammarPattern": "evaluation + of/for", "weekIntroduced": 16},
        {"word": "verification", "definition": "the process of establishing the truth or correctness", "difficulty": 7, "category": "ACADEMIC", "contexts": ["Identity verification", "Data verification"], "relatedWords": ["confirmation", "validation", "authentication"], "grammarPattern": "verification + of/process", "weekIntroduced": 19}
    ]

def add_exam_focused_vocabulary():
    return [
        # Test-taking and academic context
        {"word": "proficiency", "definition": "a high degree of skill; expertise", "difficulty": 6, "category": "EXAM_SPECIFIC", "contexts": ["Language proficiency", "Technical proficiency"], "relatedWords": ["competence", "skill", "expertise"], "grammarPattern": "proficiency + in/at", "weekIntroduced": 18},
        {"word": "comprehensive", "definition": "including everything; complete", "difficulty": 6, "category": "EXAM_SPECIFIC", "contexts": ["Comprehensive exam", "Comprehensive coverage"], "relatedWords": ["complete", "thorough", "extensive"], "grammarPattern": "comprehensive + exam/study", "weekIntroduced": 17},
        {"word": "accumulate", "definition": "to gather together or acquire an increasing number", "difficulty": 6, "category": "EXAM_SPECIFIC", "contexts": ["Accumulate knowledge", "Points accumulate over time"], "relatedWords": ["collect", "gather", "amass"], "grammarPattern": "accumulate + knowledge/points", "weekIntroduced": 16},
        {"word": "demonstrate", "definition": "to clearly show the existence or truth of something", "difficulty": 5, "category": "EXAM_SPECIFIC", "contexts": ["Demonstrate understanding", "Demonstrate skills"], "relatedWords": ["show", "prove", "exhibit"], "grammarPattern": "demonstrate + ability/knowledge", "weekIntroduced": 15},
        {"word": "equivalent", "definition": "equal in value, amount, function, or meaning", "difficulty": 6, "category": "EXAM_SPECIFIC", "contexts": ["Equivalent expressions", "Find the equivalent"], "relatedWords": ["equal", "corresponding", "comparable"], "grammarPattern": "equivalent + to/of", "weekIntroduced": 17},

        # Academic performance
        {"word": "competency", "definition": "the ability to do something successfully", "difficulty": 6, "category": "EXAM_SPECIFIC", "contexts": ["Core competency", "Language competency"], "relatedWords": ["ability", "skill", "proficiency"], "grammarPattern": "competency + in/for", "weekIntroduced": 18},
        {"word": "prerequisite", "definition": "a thing that is required beforehand", "difficulty": 7, "category": "EXAM_SPECIFIC", "contexts": ["Course prerequisite", "Basic prerequisite"], "relatedWords": ["requirement", "condition", "necessity"], "grammarPattern": "prerequisite + for/to", "weekIntroduced": 19},
        {"word": "curriculum", "definition": "the subjects in a course of study", "difficulty": 6, "category": "EXAM_SPECIFIC", "contexts": ["School curriculum", "Updated curriculum"], "relatedWords": ["syllabus", "program", "course"], "grammarPattern": "curriculum + includes/covers", "weekIntroduced": 16},
        {"word": "supplement", "definition": "something that completes or enhances", "difficulty": 5, "category": "EXAM_SPECIFIC", "contexts": ["Supplement your studies", "Additional supplement"], "relatedWords": ["addition", "complement", "enhancement"], "grammarPattern": "supplement + with/to", "weekIntroduced": 15},
        {"word": "consecutive", "definition": "following each other in uninterrupted succession", "difficulty": 6, "category": "EXAM_SPECIFIC", "contexts": ["Consecutive days", "Three consecutive wins"], "relatedWords": ["successive", "continuous", "sequential"], "grammarPattern": "consecutive + days/times", "weekIntroduced": 17},

        # Measurement and comparison
        {"word": "approximately", "definition": "used to show that something is almost correct", "difficulty": 5, "category": "EXAM_SPECIFIC", "contexts": ["Approximately 100 students", "Takes approximately 2 hours"], "relatedWords": ["about", "roughly", "nearly"], "grammarPattern": "approximately + number/time", "weekIntroduced": 14},
        {"word": "proportion", "definition": "a part considered in relation to the whole", "difficulty": 6, "category": "EXAM_SPECIFIC", "contexts": ["Large proportion of students", "In proportion to"], "relatedWords": ["ratio", "percentage", "fraction"], "grammarPattern": "proportion + of/to", "weekIntroduced": 16},
        {"word": "respectively", "definition": "in the order given", "difficulty": 6, "category": "EXAM_SPECIFIC", "contexts": ["John and Mary scored 85 and 90 respectively", "The results were 40% and 60% respectively"], "relatedWords": ["individually", "separately", "correspondingly"], "grammarPattern": "A and B respectively", "weekIntroduced": 17},
        {"word": "significantly", "definition": "in a sufficiently great way to be worthy of attention", "difficulty": 6, "category": "EXAM_SPECIFIC", "contexts": ["Significantly better results", "Scores improved significantly"], "relatedWords": ["considerably", "notably", "substantially"], "grammarPattern": "significantly + better/higher", "weekIntroduced": 16},
        {"word": "predominantly", "definition": "mainly; for the most part", "difficulty": 7, "category": "EXAM_SPECIFIC", "contexts": ["Predominantly academic content", "The audience was predominantly young"], "relatedWords": ["mainly", "mostly", "largely"], "grammarPattern": "predominantly + adjective/noun", "weekIntroduced": 18}
    ]

def add_final_vocabulary_set():
    return [
        # Advanced grammar and language terms
        {"word": "auxiliary", "definition": "providing supplementary or additional help", "difficulty": 7, "category": "GRAMMAR_FOCUSED", "contexts": ["Auxiliary verb", "Auxiliary equipment"], "relatedWords": ["helping", "supporting", "additional"], "grammarPattern": "auxiliary + verb/noun", "weekIntroduced": 20},
        {"word": "subjunctive", "definition": "relating to a grammatical mood", "difficulty": 8, "category": "GRAMMAR_FOCUSED", "contexts": ["Subjunctive mood", "Use the subjunctive"], "relatedWords": ["conditional", "hypothetical", "mood"], "grammarPattern": "subjunctive + mood/form", "weekIntroduced": 23},
        {"word": "infinitive", "definition": "the basic form of a verb", "difficulty": 6, "category": "GRAMMAR_FOCUSED", "contexts": ["Infinitive form", "To + infinitive"], "relatedWords": ["base form", "root", "basic"], "grammarPattern": "infinitive + form/verb", "weekIntroduced": 17},
        {"word": "participle", "definition": "a form of verb used as adjective", "difficulty": 7, "category": "GRAMMAR_FOCUSED", "contexts": ["Past participle", "Present participle"], "relatedWords": ["verbal", "adjective", "modifier"], "grammarPattern": "past/present + participle", "weekIntroduced": 18},
        {"word": "conditional", "definition": "expressing a condition", "difficulty": 6, "category": "GRAMMAR_FOCUSED", "contexts": ["Conditional sentence", "First conditional"], "relatedWords": ["hypothetical", "if-clause", "dependent"], "grammarPattern": "conditional + sentence/clause", "weekIntroduced": 16},

        # Final academic terms
        {"word": "fundamental", "definition": "forming a necessary base or core", "difficulty": 6, "category": "ACADEMIC", "contexts": ["Fundamental principles", "Fundamental research"], "relatedWords": ["basic", "essential", "core"], "grammarPattern": "fundamental + principle/concept", "weekIntroduced": 17},
        {"word": "constituent", "definition": "being a part of a whole", "difficulty": 7, "category": "ACADEMIC", "contexts": ["Constituent elements", "Political constituent"], "relatedWords": ["component", "element", "part"], "grammarPattern": "constituent + of/element", "weekIntroduced": 19},
        {"word": "aggregate", "definition": "formed by combining several elements", "difficulty": 7, "category": "ACADEMIC", "contexts": ["Aggregate data", "In aggregate"], "relatedWords": ["total", "combined", "collective"], "grammarPattern": "aggregate + data/total", "weekIntroduced": 20},
        {"word": "implicit", "definition": "suggested though not directly expressed", "difficulty": 7, "category": "ACADEMIC", "contexts": ["Implicit meaning", "Implicit in the statement"], "relatedWords": ["implied", "understood", "indirect"], "grammarPattern": "implicit + in/meaning", "weekIntroduced": 21},
        {"word": "explicit", "definition": "stated clearly and in detail", "difficulty": 6, "category": "ACADEMIC", "contexts": ["Explicit instructions", "Make it explicit"], "relatedWords": ["clear", "direct", "obvious"], "grammarPattern": "explicit + about/instruction", "weekIntroduced": 18},

        # Contemporary and technological terms
        {"word": "algorithm", "definition": "a set of rules to be followed in calculations", "difficulty": 7, "category": "ACADEMIC", "contexts": ["Computer algorithm", "Search algorithm"], "relatedWords": ["procedure", "method", "formula"], "grammarPattern": "algorithm + for/to", "weekIntroduced": 22},
        {"word": "interface", "definition": "a point where systems meet and interact", "difficulty": 6, "category": "ACADEMIC", "contexts": ["User interface", "Computer interface"], "relatedWords": ["connection", "boundary", "interaction"], "grammarPattern": "interface + between/with", "weekIntroduced": 19},
        {"word": "protocol", "definition": "a system of rules governing affairs", "difficulty": 6, "category": "ACADEMIC", "contexts": ["Security protocol", "Communication protocol"], "relatedWords": ["procedure", "standard", "system"], "grammarPattern": "protocol + for/of", "weekIntroduced": 18},
        {"word": "parameter", "definition": "a numerical characteristic of a population", "difficulty": 7, "category": "ACADEMIC", "contexts": ["Set parameters", "System parameters"], "relatedWords": ["variable", "setting", "limit"], "grammarPattern": "parameter + of/for", "weekIntroduced": 20},
        {"word": "configuration", "definition": "an arrangement of elements in a particular form", "difficulty": 6, "category": "ACADEMIC", "contexts": ["System configuration", "Default configuration"], "relatedWords": ["setup", "arrangement", "layout"], "grammarPattern": "configuration + of/for", "weekIntroduced": 17}
    ]

def main():
    vocab_data = load_vocabulary()
    current_count = len(vocab_data)
    print(f"Current vocabulary: {current_count} words")

    # Add all remaining vocabulary sets
    new_words = []
    new_words.extend(add_advanced_academic_terms())
    new_words.extend(add_comprehensive_academic_vocabulary())
    new_words.extend(add_exam_focused_vocabulary())
    new_words.extend(add_final_vocabulary_set())

    # Filter out duplicates
    existing_words = {item["word"].lower() for item in vocab_data}
    unique_new_words = [word for word in new_words if word["word"].lower() not in existing_words]

    vocab_data.extend(unique_new_words)

    # Save the updated database
    save_vocabulary(vocab_data)

    # Print statistics
    final_count = len(vocab_data)
    print(f"Added {len(unique_new_words)} new words")
    print(f"Total vocabulary: {final_count} words")

    # Category distribution
    from collections import Counter
    categories = Counter(item["category"] for item in vocab_data)
    print(f"Final category distribution: {dict(categories)}")

    if final_count >= 2000:
        print("✅ TARGET ACHIEVED: 2000+ words!")
    else:
        print(f"Need {2000 - final_count} more words to reach 2000")

if __name__ == "__main__":
    main()