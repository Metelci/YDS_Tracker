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

def add_extensive_academic_vocabulary():
    return [
        # Research and methodology
        {"word": "longitudinal", "definition": "relating to measurement over a long period", "difficulty": 8, "category": "ACADEMIC", "contexts": ["Longitudinal study", "Longitudinal data"], "relatedWords": ["extended", "long-term", "temporal"], "grammarPattern": "longitudinal + study/research", "weekIntroduced": 24},
        {"word": "quantitative", "definition": "relating to measurement of quantity", "difficulty": 7, "category": "ACADEMIC", "contexts": ["Quantitative analysis", "Quantitative research"], "relatedWords": ["numerical", "statistical", "measurable"], "grammarPattern": "quantitative + analysis/data", "weekIntroduced": 22},
        {"word": "qualitative", "definition": "relating to quality rather than quantity", "difficulty": 7, "category": "ACADEMIC", "contexts": ["Qualitative research", "Qualitative assessment"], "relatedWords": ["descriptive", "subjective", "interpretive"], "grammarPattern": "qualitative + research/analysis", "weekIntroduced": 22},
        {"word": "retrospective", "definition": "looking back on past events", "difficulty": 8, "category": "ACADEMIC", "contexts": ["Retrospective analysis", "In retrospective"], "relatedWords": ["backward-looking", "historical", "reflective"], "grammarPattern": "retrospective + analysis/view", "weekIntroduced": 25},
        {"word": "prospective", "definition": "relating to the future", "difficulty": 7, "category": "ACADEMIC", "contexts": ["Prospective study", "Prospective analysis"], "relatedWords": ["forward-looking", "future", "anticipated"], "grammarPattern": "prospective + study/analysis", "weekIntroduced": 24},
        {"word": "cohort", "definition": "a group of people with shared characteristics", "difficulty": 8, "category": "ACADEMIC", "contexts": ["Study cohort", "Age cohort"], "relatedWords": ["group", "category", "demographic"], "grammarPattern": "cohort + study/group", "weekIntroduced": 26},
        {"word": "demographic", "definition": "relating to population characteristics", "difficulty": 7, "category": "ACADEMIC", "contexts": ["Demographic analysis", "Demographic trends"], "relatedWords": ["population", "statistical", "social"], "grammarPattern": "demographic + data/analysis", "weekIntroduced": 23},
        {"word": "statistical", "definition": "relating to statistics", "difficulty": 6, "category": "ACADEMIC", "contexts": ["Statistical significance", "Statistical analysis"], "relatedWords": ["numerical", "mathematical", "data-based"], "grammarPattern": "statistical + significance/analysis", "weekIntroduced": 21},
        {"word": "normative", "definition": "establishing a standard or norm", "difficulty": 8, "category": "ACADEMIC", "contexts": ["Normative behavior", "Normative standards"], "relatedWords": ["standard", "typical", "prescribed"], "grammarPattern": "normative + behavior/standard", "weekIntroduced": 26},
        {"word": "descriptive", "definition": "serving to describe", "difficulty": 5, "category": "ACADEMIC", "contexts": ["Descriptive statistics", "Descriptive analysis"], "relatedWords": ["explanatory", "illustrative", "characterizing"], "grammarPattern": "descriptive + statistics/analysis", "weekIntroduced": 19},

        # Philosophy and abstract concepts
        {"word": "epistemology", "definition": "the theory of knowledge", "difficulty": 9, "category": "ACADEMIC", "contexts": ["Study epistemology", "Epistemological questions"], "relatedWords": ["knowledge", "philosophy", "theory"], "grammarPattern": "epistemology + of/in", "weekIntroduced": 28},
        {"word": "ontology", "definition": "the branch of philosophy dealing with existence", "difficulty": 9, "category": "ACADEMIC", "contexts": ["Ontological questions", "Study ontology"], "relatedWords": ["existence", "being", "philosophy"], "grammarPattern": "ontology + of/in", "weekIntroduced": 28},
        {"word": "phenomenology", "definition": "the study of consciousness and experience", "difficulty": 9, "category": "ACADEMIC", "contexts": ["Phenomenological approach", "Study phenomenology"], "relatedWords": ["experience", "consciousness", "philosophy"], "grammarPattern": "phenomenology + of/in", "weekIntroduced": 29},
        {"word": "dialectical", "definition": "relating to logical discussion", "difficulty": 8, "category": "ACADEMIC", "contexts": ["Dialectical approach", "Dialectical reasoning"], "relatedWords": ["logical", "argumentative", "reasoned"], "grammarPattern": "dialectical + approach/method", "weekIntroduced": 27},
        {"word": "dichotomy", "definition": "division into two contrasting groups", "difficulty": 8, "category": "ACADEMIC", "contexts": ["False dichotomy", "Create a dichotomy"], "relatedWords": ["division", "contrast", "separation"], "grammarPattern": "dichotomy + between/of", "weekIntroduced": 25},
        {"word": "paradox", "definition": "a seemingly contradictory statement", "difficulty": 7, "category": "ACADEMIC", "contexts": ["Logical paradox", "The paradox of choice"], "relatedWords": ["contradiction", "puzzle", "anomaly"], "grammarPattern": "paradox + of/in", "weekIntroduced": 24},
        {"word": "antithesis", "definition": "the direct opposite", "difficulty": 8, "category": "ACADEMIC", "contexts": ["Complete antithesis", "The antithesis of success"], "relatedWords": ["opposite", "contrast", "reverse"], "grammarPattern": "antithesis + of/to", "weekIntroduced": 26},
        {"word": "synthesis", "definition": "combination of ideas to form a theory", "difficulty": 8, "category": "ACADEMIC", "contexts": ["Synthesis of ideas", "Theoretical synthesis"], "relatedWords": ["combination", "integration", "fusion"], "grammarPattern": "synthesis + of/between", "weekIntroduced": 25},
        {"word": "ideology", "definition": "a system of ideas and ideals", "difficulty": 7, "category": "ACADEMIC", "contexts": ["Political ideology", "Dominant ideology"], "relatedWords": ["philosophy", "doctrine", "belief"], "grammarPattern": "ideology + of/behind", "weekIntroduced": 23},
        {"word": "hegemony", "definition": "dominance of one group over others", "difficulty": 9, "category": "ACADEMIC", "contexts": ["Cultural hegemony", "Economic hegemony"], "relatedWords": ["dominance", "supremacy", "control"], "grammarPattern": "hegemony + of/over", "weekIntroduced": 28},

        # Scientific and technical
        {"word": "biomechanics", "definition": "the study of mechanical laws relating to living organisms", "difficulty": 8, "category": "ACADEMIC", "contexts": ["Study biomechanics", "Biomechanical analysis"], "relatedWords": ["mechanics", "biology", "movement"], "grammarPattern": "biomechanics + of/in", "weekIntroduced": 26},
        {"word": "thermodynamics", "definition": "the branch of physics dealing with heat and temperature", "difficulty": 8, "category": "ACADEMIC", "contexts": ["Laws of thermodynamics", "Thermodynamic principles"], "relatedWords": ["physics", "energy", "heat"], "grammarPattern": "thermodynamics + law/principle", "weekIntroduced": 27},
        {"word": "electromagnetic", "definition": "relating to electricity and magnetism", "difficulty": 7, "category": "ACADEMIC", "contexts": ["Electromagnetic radiation", "Electromagnetic field"], "relatedWords": ["electrical", "magnetic", "radiation"], "grammarPattern": "electromagnetic + field/radiation", "weekIntroduced": 25},
        {"word": "biochemistry", "definition": "the study of chemical processes in living organisms", "difficulty": 7, "category": "ACADEMIC", "contexts": ["Study biochemistry", "Biochemical processes"], "relatedWords": ["chemistry", "biology", "molecular"], "grammarPattern": "biochemistry + of/in", "weekIntroduced": 24},
        {"word": "physiology", "definition": "the study of functions of living organisms", "difficulty": 7, "category": "ACADEMIC", "contexts": ["Human physiology", "Plant physiology"], "relatedWords": ["biology", "function", "organism"], "grammarPattern": "physiology + of/in", "weekIntroduced": 23},
        {"word": "pathology", "definition": "the study of disease", "difficulty": 7, "category": "ACADEMIC", "contexts": ["Study pathology", "Disease pathology"], "relatedWords": ["disease", "medicine", "diagnosis"], "grammarPattern": "pathology + of/in", "weekIntroduced": 24},
        {"word": "pharmacology", "definition": "the study of drugs and their effects", "difficulty": 8, "category": "ACADEMIC", "contexts": ["Study pharmacology", "Drug pharmacology"], "relatedWords": ["medicine", "drugs", "treatment"], "grammarPattern": "pharmacology + of/in", "weekIntroduced": 25},
        {"word": "morphology", "definition": "the study of form and structure", "difficulty": 8, "category": "ACADEMIC", "contexts": ["Word morphology", "Cell morphology"], "relatedWords": ["structure", "form", "shape"], "grammarPattern": "morphology + of/in", "weekIntroduced": 26},
        {"word": "etymology", "definition": "the study of word origins", "difficulty": 7, "category": "ACADEMIC", "contexts": ["Word etymology", "Study etymology"], "relatedWords": ["origin", "history", "language"], "grammarPattern": "etymology + of/in", "weekIntroduced": 24},
        {"word": "taxonomy", "definition": "the classification of things", "difficulty": 7, "category": "ACADEMIC", "contexts": ["Biological taxonomy", "Classification taxonomy"], "relatedWords": ["classification", "category", "organization"], "grammarPattern": "taxonomy + of/in", "weekIntroduced": 25},

        # Psychology and sociology
        {"word": "cognition", "definition": "the mental action of acquiring knowledge", "difficulty": 7, "category": "ACADEMIC", "contexts": ["Human cognition", "Cognitive processes"], "relatedWords": ["thinking", "perception", "understanding"], "grammarPattern": "cognition + in/of", "weekIntroduced": 23},
        {"word": "metacognition", "definition": "awareness of one's thought processes", "difficulty": 8, "category": "ACADEMIC", "contexts": ["Develop metacognition", "Metacognitive strategies"], "relatedWords": ["self-awareness", "reflection", "thinking"], "grammarPattern": "metacognition + in/about", "weekIntroduced": 26},
        {"word": "socialization", "definition": "the process of learning social norms", "difficulty": 6, "category": "ACADEMIC", "contexts": ["Childhood socialization", "Cultural socialization"], "relatedWords": ["learning", "adaptation", "culture"], "grammarPattern": "socialization + into/through", "weekIntroduced": 21},
        {"word": "stratification", "definition": "division into layers or classes", "difficulty": 8, "category": "ACADEMIC", "contexts": ["Social stratification", "Economic stratification"], "relatedWords": ["hierarchy", "division", "class"], "grammarPattern": "stratification + of/in", "weekIntroduced": 25},
        {"word": "marginalization", "definition": "treatment as insignificant", "difficulty": 7, "category": "ACADEMIC", "contexts": ["Social marginalization", "Economic marginalization"], "relatedWords": ["exclusion", "isolation", "discrimination"], "grammarPattern": "marginalization + of/in", "weekIntroduced": 24},
        {"word": "institutionalization", "definition": "establishment as a norm in an organization", "difficulty": 8, "category": "ACADEMIC", "contexts": ["Institutionalization of practices", "Social institutionalization"], "relatedWords": ["establishment", "normalization", "systematization"], "grammarPattern": "institutionalization + of/in", "weekIntroduced": 27},
        {"word": "globalization", "definition": "the process of international integration", "difficulty": 7, "category": "ACADEMIC", "contexts": ["Economic globalization", "Cultural globalization"], "relatedWords": ["integration", "worldwide", "international"], "grammarPattern": "globalization + of/in", "weekIntroduced": 23},
        {"word": "urbanization", "definition": "the process of making an area more urban", "difficulty": 6, "category": "ACADEMIC", "contexts": ["Rapid urbanization", "Urban planning"], "relatedWords": ["development", "city", "growth"], "grammarPattern": "urbanization + of/in", "weekIntroduced": 22},
        {"word": "industrialization", "definition": "development of industries in a region", "difficulty": 7, "category": "ACADEMIC", "contexts": ["Industrial revolution", "Economic industrialization"], "relatedWords": ["development", "manufacturing", "economy"], "grammarPattern": "industrialization + of/in", "weekIntroduced": 23},
        {"word": "democratization", "definition": "introduction of democratic system", "difficulty": 7, "category": "ACADEMIC", "contexts": ["Political democratization", "Democratization process"], "relatedWords": ["democracy", "political", "freedom"], "grammarPattern": "democratization + of/in", "weekIntroduced": 24}
    ]

def add_extensive_business_vocabulary():
    return [
        # Finance and economics
        {"word": "depreciation", "definition": "decrease in value over time", "difficulty": 7, "category": "BUSINESS", "contexts": ["Asset depreciation", "Currency depreciation"], "relatedWords": ["decline", "devaluation", "reduction"], "grammarPattern": "depreciation + of/in", "weekIntroduced": 23},
        {"word": "appreciation", "definition": "increase in value over time", "difficulty": 6, "category": "BUSINESS", "contexts": ["Asset appreciation", "Currency appreciation"], "relatedWords": ["increase", "growth", "rise"], "grammarPattern": "appreciation + of/in", "weekIntroduced": 22},
        {"word": "liquidity", "definition": "availability of liquid assets", "difficulty": 7, "category": "BUSINESS", "contexts": ["Market liquidity", "Cash liquidity"], "relatedWords": ["cash", "availability", "convertibility"], "grammarPattern": "liquidity + of/in", "weekIntroduced": 24},
        {"word": "volatility", "definition": "tendency to change rapidly", "difficulty": 7, "category": "BUSINESS", "contexts": ["Market volatility", "Price volatility"], "relatedWords": ["instability", "fluctuation", "variability"], "grammarPattern": "volatility + in/of", "weekIntroduced": 25},
        {"word": "equity", "definition": "ownership interest in a company", "difficulty": 6, "category": "BUSINESS", "contexts": ["Equity investment", "Shareholder equity"], "relatedWords": ["ownership", "shares", "stake"], "grammarPattern": "equity + in/of", "weekIntroduced": 21},
        {"word": "liability", "definition": "financial obligation or debt", "difficulty": 6, "category": "BUSINESS", "contexts": ["Current liability", "Legal liability"], "relatedWords": ["debt", "obligation", "responsibility"], "grammarPattern": "liability + for/of", "weekIntroduced": 22},
        {"word": "profitability", "definition": "ability to generate profit", "difficulty": 6, "category": "BUSINESS", "contexts": ["Company profitability", "Project profitability"], "relatedWords": ["profit", "earnings", "return"], "grammarPattern": "profitability + of/in", "weekIntroduced": 21},
        {"word": "solvency", "definition": "ability to meet long-term obligations", "difficulty": 8, "category": "BUSINESS", "contexts": ["Company solvency", "Financial solvency"], "relatedWords": ["stability", "viability", "soundness"], "grammarPattern": "solvency + of/in", "weekIntroduced": 26},
        {"word": "insolvency", "definition": "inability to pay debts", "difficulty": 8, "category": "BUSINESS", "contexts": ["Corporate insolvency", "Personal insolvency"], "relatedWords": ["bankruptcy", "default", "failure"], "grammarPattern": "insolvency + of/in", "weekIntroduced": 27},
        {"word": "bankruptcy", "definition": "legal state of being unable to pay debts", "difficulty": 6, "category": "BUSINESS", "contexts": ["File for bankruptcy", "Bankruptcy proceedings"], "relatedWords": ["insolvency", "failure", "default"], "grammarPattern": "bankruptcy + proceedings/filing", "weekIntroduced": 23},

        # Management and strategy
        {"word": "accountability", "definition": "responsibility for decisions and actions", "difficulty": 6, "category": "BUSINESS", "contexts": ["Corporate accountability", "Personal accountability"], "relatedWords": ["responsibility", "answerability", "liability"], "grammarPattern": "accountability + for/in", "weekIntroduced": 21},
        {"word": "transparency", "definition": "openness and honesty in business", "difficulty": 6, "category": "BUSINESS", "contexts": ["Financial transparency", "Corporate transparency"], "relatedWords": ["openness", "clarity", "honesty"], "grammarPattern": "transparency + in/of", "weekIntroduced": 22},
        {"word": "sustainability", "definition": "ability to maintain operations long-term", "difficulty": 7, "category": "BUSINESS", "contexts": ["Business sustainability", "Environmental sustainability"], "relatedWords": ["durability", "viability", "continuity"], "grammarPattern": "sustainability + of/in", "weekIntroduced": 24},
        {"word": "scalability", "definition": "ability to handle increased workload", "difficulty": 7, "category": "BUSINESS", "contexts": ["Business scalability", "System scalability"], "relatedWords": ["expandability", "growth", "adaptability"], "grammarPattern": "scalability + of/in", "weekIntroduced": 25},
        {"word": "viability", "definition": "ability to work successfully", "difficulty": 6, "category": "BUSINESS", "contexts": ["Project viability", "Business viability"], "relatedWords": ["feasibility", "practicality", "workability"], "grammarPattern": "viability + of/in", "weekIntroduced": 23},
        {"word": "feasibility", "definition": "state of being easily achieved", "difficulty": 6, "category": "BUSINESS", "contexts": ["Project feasibility", "Economic feasibility"], "relatedWords": ["practicality", "possibility", "achievability"], "grammarPattern": "feasibility + of/study", "weekIntroduced": 22},
        {"word": "efficiency", "definition": "achieving maximum productivity", "difficulty": 5, "category": "BUSINESS", "contexts": ["Operational efficiency", "Cost efficiency"], "relatedWords": ["productivity", "effectiveness", "optimization"], "grammarPattern": "efficiency + in/of", "weekIntroduced": 19},
        {"word": "effectiveness", "definition": "degree of success in producing results", "difficulty": 5, "category": "BUSINESS", "contexts": ["Marketing effectiveness", "Policy effectiveness"], "relatedWords": ["success", "impact", "results"], "grammarPattern": "effectiveness + of/in", "weekIntroduced": 20},
        {"word": "synergy", "definition": "combined effect greater than sum of parts", "difficulty": 7, "category": "BUSINESS", "contexts": ["Team synergy", "Business synergy"], "relatedWords": ["cooperation", "collaboration", "combination"], "grammarPattern": "synergy + between/in", "weekIntroduced": 24},
        {"word": "leverage", "definition": "use of borrowed capital for investment", "difficulty": 6, "category": "BUSINESS", "contexts": ["Financial leverage", "Leverage resources"], "relatedWords": ["advantage", "influence", "power"], "grammarPattern": "leverage + for/in", "weekIntroduced": 23},

        # Marketing and sales
        {"word": "segmentation", "definition": "division into distinct groups", "difficulty": 6, "category": "BUSINESS", "contexts": ["Market segmentation", "Customer segmentation"], "relatedWords": ["division", "categorization", "classification"], "grammarPattern": "segmentation + of/in", "weekIntroduced": 22},
        {"word": "differentiation", "definition": "distinguishing from competitors", "difficulty": 6, "category": "BUSINESS", "contexts": ["Product differentiation", "Brand differentiation"], "relatedWords": ["distinction", "uniqueness", "specialization"], "grammarPattern": "differentiation + from/of", "weekIntroduced": 23},
        {"word": "positioning", "definition": "creating a distinct image in customers' minds", "difficulty": 6, "category": "BUSINESS", "contexts": ["Brand positioning", "Market positioning"], "relatedWords": ["placement", "image", "perception"], "grammarPattern": "positioning + of/in", "weekIntroduced": 22},
        {"word": "penetration", "definition": "extent of market share achieved", "difficulty": 6, "category": "BUSINESS", "contexts": ["Market penetration", "Price penetration"], "relatedWords": ["entry", "infiltration", "reach"], "grammarPattern": "penetration + of/in", "weekIntroduced": 24},
        {"word": "saturation", "definition": "point where market cannot absorb more", "difficulty": 7, "category": "BUSINESS", "contexts": ["Market saturation", "Product saturation"], "relatedWords": ["fullness", "capacity", "limit"], "grammarPattern": "saturation + of/in", "weekIntroduced": 25},
        {"word": "obsolescence", "definition": "state of being outdated", "difficulty": 7, "category": "BUSINESS", "contexts": ["Product obsolescence", "Planned obsolescence"], "relatedWords": ["outdated", "superseded", "discontinued"], "grammarPattern": "obsolescence + of/in", "weekIntroduced": 26},
        {"word": "innovation", "definition": "introduction of new ideas or methods", "difficulty": 5, "category": "BUSINESS", "contexts": ["Product innovation", "Process innovation"], "relatedWords": ["creativity", "invention", "novelty"], "grammarPattern": "innovation + in/of", "weekIntroduced": 18},
        {"word": "commercialization", "definition": "process of introducing new product to market", "difficulty": 7, "category": "BUSINESS", "contexts": ["Product commercialization", "Technology commercialization"], "relatedWords": ["marketing", "introduction", "launch"], "grammarPattern": "commercialization + of/in", "weekIntroduced": 25},
        {"word": "monetization", "definition": "process of generating revenue", "difficulty": 7, "category": "BUSINESS", "contexts": ["Data monetization", "Asset monetization"], "relatedWords": ["revenue", "profit", "income"], "grammarPattern": "monetization + of/through", "weekIntroduced": 26},
        {"word": "optimization", "definition": "making best use of situation or resource", "difficulty": 6, "category": "BUSINESS", "contexts": ["Process optimization", "Cost optimization"], "relatedWords": ["improvement", "enhancement", "maximization"], "grammarPattern": "optimization + of/for", "weekIntroduced": 23}
    ]

def add_extensive_exam_vocabulary():
    return [
        # Academic performance and testing
        {"word": "assessment", "definition": "evaluation or estimation of nature or quality", "difficulty": 5, "category": "EXAM_SPECIFIC", "contexts": ["Performance assessment", "Risk assessment"], "relatedWords": ["evaluation", "appraisal", "analysis"], "grammarPattern": "assessment + of/for", "weekIntroduced": 17},
        {"word": "evaluation", "definition": "making of judgment about amount or value", "difficulty": 5, "category": "EXAM_SPECIFIC", "contexts": ["Project evaluation", "Student evaluation"], "relatedWords": ["assessment", "appraisal", "review"], "grammarPattern": "evaluation + of/for", "weekIntroduced": 16},
        {"word": "comprehension", "definition": "ability to understand", "difficulty": 6, "category": "EXAM_SPECIFIC", "contexts": ["Reading comprehension", "Listening comprehension"], "relatedWords": ["understanding", "grasp", "perception"], "grammarPattern": "comprehension + of/in", "weekIntroduced": 18},
        {"word": "interpretation", "definition": "explanation of meaning", "difficulty": 6, "category": "EXAM_SPECIFIC", "contexts": ["Data interpretation", "Text interpretation"], "relatedWords": ["explanation", "analysis", "understanding"], "grammarPattern": "interpretation + of/for", "weekIntroduced": 19},
        {"word": "synthesis", "definition": "combination of ideas to form theory", "difficulty": 7, "category": "EXAM_SPECIFIC", "contexts": ["Information synthesis", "Knowledge synthesis"], "relatedWords": ["combination", "integration", "merger"], "grammarPattern": "synthesis + of/from", "weekIntroduced": 21},
        {"word": "analysis", "definition": "detailed examination of structure", "difficulty": 5, "category": "EXAM_SPECIFIC", "contexts": ["Statistical analysis", "Text analysis"], "relatedWords": ["examination", "study", "investigation"], "grammarPattern": "analysis + of/for", "weekIntroduced": 16},
        {"word": "application", "definition": "practical use of something", "difficulty": 5, "category": "EXAM_SPECIFIC", "contexts": ["Knowledge application", "Skill application"], "relatedWords": ["use", "implementation", "practice"], "grammarPattern": "application + of/in", "weekIntroduced": 17},
        {"word": "demonstration", "definition": "action of showing something exists", "difficulty": 5, "category": "EXAM_SPECIFIC", "contexts": ["Skill demonstration", "Knowledge demonstration"], "relatedWords": ["showing", "proof", "exhibition"], "grammarPattern": "demonstration + of/for", "weekIntroduced": 16},
        {"word": "implementation", "definition": "process of putting decision into effect", "difficulty": 6, "category": "EXAM_SPECIFIC", "contexts": ["Plan implementation", "Strategy implementation"], "relatedWords": ["execution", "application", "realization"], "grammarPattern": "implementation + of/for", "weekIntroduced": 18},
        {"word": "consolidation", "definition": "action of making something stronger", "difficulty": 7, "category": "EXAM_SPECIFIC", "contexts": ["Knowledge consolidation", "Learning consolidation"], "relatedWords": ["strengthening", "reinforcement", "solidification"], "grammarPattern": "consolidation + of/in", "weekIntroduced": 20},

        # Language learning specific
        {"word": "fluency", "definition": "ability to speak easily and accurately", "difficulty": 5, "category": "EXAM_SPECIFIC", "contexts": ["Language fluency", "Reading fluency"], "relatedWords": ["proficiency", "skill", "competence"], "grammarPattern": "fluency + in/of", "weekIntroduced": 16},
        {"word": "accuracy", "definition": "quality of being correct or precise", "difficulty": 5, "category": "EXAM_SPECIFIC", "contexts": ["Grammatical accuracy", "Pronunciation accuracy"], "relatedWords": ["correctness", "precision", "exactness"], "grammarPattern": "accuracy + in/of", "weekIntroduced": 15},
        {"word": "coherence", "definition": "quality of being logical and consistent", "difficulty": 6, "category": "EXAM_SPECIFIC", "contexts": ["Text coherence", "Argument coherence"], "relatedWords": ["consistency", "logic", "unity"], "grammarPattern": "coherence + in/of", "weekIntroduced": 18},
        {"word": "cohesion", "definition": "action of forming united whole", "difficulty": 6, "category": "EXAM_SPECIFIC", "contexts": ["Text cohesion", "Group cohesion"], "relatedWords": ["unity", "connection", "bond"], "grammarPattern": "cohesion + in/of", "weekIntroduced": 19},
        {"word": "discourse", "definition": "written or spoken communication", "difficulty": 6, "category": "EXAM_SPECIFIC", "contexts": ["Academic discourse", "Public discourse"], "relatedWords": ["communication", "discussion", "conversation"], "grammarPattern": "discourse + on/about", "weekIntroduced": 19},
        {"word": "rhetoric", "definition": "art of effective speaking or writing", "difficulty": 7, "category": "EXAM_SPECIFIC", "contexts": ["Political rhetoric", "Academic rhetoric"], "relatedWords": ["eloquence", "oratory", "persuasion"], "grammarPattern": "rhetoric + of/in", "weekIntroduced": 22},
        {"word": "collocation", "definition": "habitual juxtaposition of particular word", "difficulty": 7, "category": "EXAM_SPECIFIC", "contexts": ["Word collocation", "Learn collocations"], "relatedWords": ["combination", "pairing", "association"], "grammarPattern": "collocation + with/of", "weekIntroduced": 21},
        {"word": "connotation", "definition": "idea suggested by word in addition to meaning", "difficulty": 6, "category": "EXAM_SPECIFIC", "contexts": ["Positive connotation", "Cultural connotation"], "relatedWords": ["implication", "suggestion", "association"], "grammarPattern": "connotation + of/in", "weekIntroduced": 20},
        {"word": "denotation", "definition": "literal meaning of word", "difficulty": 7, "category": "EXAM_SPECIFIC", "contexts": ["Word denotation", "Literal denotation"], "relatedWords": ["meaning", "definition", "reference"], "grammarPattern": "denotation + of/in", "weekIntroduced": 21},
        {"word": "register", "definition": "variety of language used in particular situation", "difficulty": 6, "category": "EXAM_SPECIFIC", "contexts": ["Formal register", "Academic register"], "relatedWords": ["style", "variety", "level"], "grammarPattern": "register + of/in", "weekIntroduced": 20}
    ]

def add_extensive_everyday_vocabulary():
    return [
        # Personal development and character
        {"word": "resilience", "definition": "ability to recover quickly from difficulties", "difficulty": 6, "category": "EVERYDAY", "contexts": ["Show resilience", "Build resilience"], "relatedWords": ["strength", "recovery", "endurance"], "grammarPattern": "resilience + in/to", "weekIntroduced": 20},
        {"word": "perseverance", "definition": "persistence in doing something despite difficulty", "difficulty": 6, "category": "EVERYDAY", "contexts": ["Show perseverance", "Require perseverance"], "relatedWords": ["persistence", "determination", "tenacity"], "grammarPattern": "perseverance + in/with", "weekIntroduced": 21},
        {"word": "diligence", "definition": "careful and persistent work or effort", "difficulty": 6, "category": "EVERYDAY", "contexts": ["Work with diligence", "Show diligence"], "relatedWords": ["carefulness", "thoroughness", "attention"], "grammarPattern": "diligence + in/with", "weekIntroduced": 19},
        {"word": "prudence", "definition": "quality of being prudent; cautiousness", "difficulty": 7, "category": "EVERYDAY", "contexts": ["Financial prudence", "Show prudence"], "relatedWords": ["caution", "wisdom", "discretion"], "grammarPattern": "prudence + in/with", "weekIntroduced": 22},
        {"word": "temperance", "definition": "moderation in action or thought", "difficulty": 8, "category": "EVERYDAY", "contexts": ["Practice temperance", "Show temperance"], "relatedWords": ["moderation", "restraint", "self-control"], "grammarPattern": "temperance + in/with", "weekIntroduced": 25},
        {"word": "fortitude", "definition": "courage in pain or adversity", "difficulty": 8, "category": "EVERYDAY", "contexts": ["Show fortitude", "Moral fortitude"], "relatedWords": ["courage", "strength", "bravery"], "grammarPattern": "fortitude + in/against", "weekIntroduced": 26},
        {"word": "eloquence", "definition": "fluent or persuasive speaking or writing", "difficulty": 7, "category": "EVERYDAY", "contexts": ["Speak with eloquence", "Natural eloquence"], "relatedWords": ["fluency", "articulateness", "persuasiveness"], "grammarPattern": "eloquence + in/of", "weekIntroduced": 24},
        {"word": "charisma", "definition": "compelling attractiveness or charm", "difficulty": 6, "category": "EVERYDAY", "contexts": ["Natural charisma", "Personal charisma"], "relatedWords": ["charm", "appeal", "magnetism"], "grammarPattern": "charisma + of/in", "weekIntroduced": 21},
        {"word": "integrity", "definition": "quality of being honest and having strong principles", "difficulty": 6, "category": "EVERYDAY", "contexts": ["Personal integrity", "Professional integrity"], "relatedWords": ["honesty", "principle", "morality"], "grammarPattern": "integrity + in/of", "weekIntroduced": 20},
        {"word": "empathy", "definition": "ability to understand others' feelings", "difficulty": 5, "category": "EVERYDAY", "contexts": ["Show empathy", "Develop empathy"], "relatedWords": ["understanding", "compassion", "sympathy"], "grammarPattern": "empathy + for/with", "weekIntroduced": 18},

        # Social interactions
        {"word": "diplomacy", "definition": "skill in dealing with people", "difficulty": 6, "category": "EVERYDAY", "contexts": ["Use diplomacy", "International diplomacy"], "relatedWords": ["tact", "skill", "negotiation"], "grammarPattern": "diplomacy + in/with", "weekIntroduced": 21},
        {"word": "etiquette", "definition": "conventional requirements of social behavior", "difficulty": 6, "category": "EVERYDAY", "contexts": ["Social etiquette", "Business etiquette"], "relatedWords": ["manners", "protocol", "courtesy"], "grammarPattern": "etiquette + of/in", "weekIntroduced": 20},
        {"word": "courtesy", "definition": "polite behavior", "difficulty": 5, "category": "EVERYDAY", "contexts": ["Show courtesy", "Common courtesy"], "relatedWords": ["politeness", "respect", "civility"], "grammarPattern": "courtesy + to/of", "weekIntroduced": 17},
        {"word": "hospitality", "definition": "friendly reception of guests", "difficulty": 5, "category": "EVERYDAY", "contexts": ["Show hospitality", "Southern hospitality"], "relatedWords": ["welcome", "friendliness", "warmth"], "grammarPattern": "hospitality + to/toward", "weekIntroduced": 18},
        {"word": "camaraderie", "definition": "mutual trust and friendship", "difficulty": 7, "category": "EVERYDAY", "contexts": ["Team camaraderie", "Build camaraderie"], "relatedWords": ["friendship", "fellowship", "companionship"], "grammarPattern": "camaraderie + among/between", "weekIntroduced": 23},
        {"word": "solidarity", "definition": "unity based on shared interests", "difficulty": 6, "category": "EVERYDAY", "contexts": ["Show solidarity", "Group solidarity"], "relatedWords": ["unity", "support", "fellowship"], "grammarPattern": "solidarity + with/among", "weekIntroduced": 22},
        {"word": "reciprocity", "definition": "practice of exchanging for mutual benefit", "difficulty": 7, "category": "EVERYDAY", "contexts": ["Social reciprocity", "Principle of reciprocity"], "relatedWords": ["exchange", "mutuality", "give-and-take"], "grammarPattern": "reciprocity + in/between", "weekIntroduced": 24},
        {"word": "compatibility", "definition": "state of being able to exist together", "difficulty": 6, "category": "EVERYDAY", "contexts": ["Personal compatibility", "System compatibility"], "relatedWords": ["harmony", "agreement", "suitability"], "grammarPattern": "compatibility + with/between", "weekIntroduced": 21},
        {"word": "versatility", "definition": "ability to adapt to many different functions", "difficulty": 6, "category": "EVERYDAY", "contexts": ["Show versatility", "Personal versatility"], "relatedWords": ["adaptability", "flexibility", "multi-purpose"], "grammarPattern": "versatility + in/of", "weekIntroduced": 22},
        {"word": "spontaneity", "definition": "quality of being spontaneous", "difficulty": 6, "category": "EVERYDAY", "contexts": ["Act with spontaneity", "Natural spontaneity"], "relatedWords": ["impulsiveness", "naturalness", "freedom"], "grammarPattern": "spontaneity + in/of", "weekIntroduced": 23}
    ]

def main():
    vocab_data = load_vocabulary()
    current_count = len(vocab_data)
    print(f"Current vocabulary: {current_count} words")

    # Add all remaining vocabulary sets
    new_words = []
    new_words.extend(add_extensive_academic_vocabulary())
    new_words.extend(add_extensive_business_vocabulary())
    new_words.extend(add_extensive_exam_vocabulary())
    new_words.extend(add_extensive_everyday_vocabulary())

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