import json
import random

def generate_2000_plus_vocabulary():
    """Generate comprehensive 2000+ vocabulary database"""

    # Load existing vocabulary to avoid duplicates
    existing_words = set()
    all_vocab = []

    try:
        # Load existing vocabulary
        with open('app/src/main/assets/vocabulary_database.json', 'r', encoding='utf-8') as f:
            existing = json.load(f)
        all_vocab.extend(existing)
        existing_words.update(item['word'] for item in existing)

        # Load previous expansions
        expansions = [
            'vocabulary_expansion_batch1.json',
            'comprehensive_vocabulary_expansion.json',
            'massive_vocabulary_database.json',
            'final_vocabulary_expansion.json'
        ]

        for filename in expansions:
            try:
                with open(filename, 'r', encoding='utf-8') as f:
                    data = json.load(f)
                new_words = [item for item in data if item['word'] not in existing_words]
                all_vocab.extend(new_words)
                existing_words.update(item['word'] for item in new_words)
            except:
                pass

        print(f"Starting with {len(existing_words)} existing words")
    except:
        print("Starting fresh")

    # Massive word lists for systematic generation
    def create_entry(word, definition, category, difficulty, week, contexts=None, related=None, pattern=None):
        if contexts is None:
            contexts = [f"The {word} demonstrates important characteristics.", f"This {word} is significant in the context."]
        if related is None:
            related = ["related", "associated", "connected"]

        return {
            "word": word,
            "definition": definition,
            "difficulty": difficulty,
            "category": category,
            "contexts": contexts,
            "relatedWords": related,
            "grammarPattern": pattern,
            "weekIntroduced": week
        }

    # Academic vocabulary - comprehensive list
    academic_words = [
        # Research methodology (100 words)
        ("abstract", "summary of research paper", 3, 12), ("academic", "relating to education and scholarship", 2, 8),
        ("accurate", "free from error", 2, 9), ("achieve", "successfully complete", 2, 7),
        ("acquire", "obtain or get", 2, 8), ("adequate", "sufficient for requirements", 2, 9),
        ("adjacent", "next to and connected with", 3, 11), ("adjust", "alter slightly for accuracy", 2, 9),
        ("administration", "process of organizing", 3, 12), ("advocate", "publicly support", 3, 13),
        ("allocate", "distribute resources", 3, 12), ("alternative", "available as another choice", 2, 8),
        ("ambiguous", "having more than one meaning", 4, 15), ("analogous", "comparable in certain respects", 4, 16),
        ("analysis", "detailed examination", 3, 10), ("analyze", "examine in detail", 2, 8),
        ("annual", "occurring once per year", 2, 9), ("anticipate", "expect or predict", 3, 11),
        ("apparent", "clearly visible or understood", 2, 10), ("append", "add as supplement", 3, 13),
        ("approach", "way of dealing with something", 2, 9), ("appropriate", "suitable for particular situation", 2, 8),
        ("approximate", "close to exact amount", 3, 11), ("arbitrary", "based on random choice", 4, 16),
        ("area", "region or part of place", 1, 6), ("aspect", "particular part or feature", 2, 10),
        ("assess", "evaluate or estimate", 3, 11), ("assign", "allocate task or role", 2, 9),
        ("assist", "help someone", 1, 7), ("assume", "suppose to be case", 2, 10),
        ("assure", "tell confidently", 2, 9), ("attach", "fasten or join", 2, 8),
        ("attain", "succeed in achieving", 3, 12), ("attitude", "settled way of thinking", 2, 9),
        ("attribute", "quality or characteristic", 3, 11), ("author", "writer of book", 2, 8),
        ("authority", "power to give orders", 2, 10), ("available", "able to be used", 1, 7),
        ("aware", "having knowledge of", 2, 8), ("benefit", "advantage gained", 2, 9),
        ("bias", "prejudice for or against", 3, 12), ("brief", "lasting only short time", 2, 8),
        ("bulk", "mass or size of something", 2, 10), ("capacity", "maximum amount containable", 2, 10),
        ("category", "class or division", 2, 9), ("cease", "bring to an end", 3, 12),
        ("challenge", "demanding task", 2, 9), ("channel", "direct toward particular end", 3, 11),
        ("chapter", "section of book", 1, 6), ("chart", "diagram showing information", 2, 8),
        ("cite", "quote as evidence", 3, 13), ("civil", "relating to citizens", 2, 10),
        ("clarify", "make easier to understand", 2, 10), ("classic", "judged over time to be highest quality", 2, 9),
        ("clause", "unit of grammatical organization", 3, 12), ("code", "system of signals", 2, 9),
        ("coherent", "logical and consistent", 3, 13), ("coincide", "occur at same time", 3, 12),
        ("collapse", "fall down suddenly", 2, 11), ("colleague", "person worked with", 2, 9),
        ("commence", "begin", 3, 12), ("comment", "verbal or written remark", 2, 8),
        ("commission", "instruction to produce", 3, 13), ("commit", "pledge to certain course", 2, 10),
        ("commodity", "raw material or product", 3, 12), ("communicate", "convey information", 2, 8),
        ("community", "group living in same place", 1, 7), ("compatible", "able to exist together", 3, 12),
        ("compensate", "give payment", 3, 13), ("compile", "collect and assemble", 3, 12),
        ("complement", "complete or make perfect", 3, 13), ("complex", "consisting of many parts", 2, 10),
        ("component", "part of larger whole", 3, 11), ("compound", "thing composed of elements", 3, 12),
        ("comprehensive", "complete and including everything", 3, 11), ("comprise", "consist of", 3, 12),
        ("compute", "calculate", 2, 10), ("conceive", "form idea in mind", 3, 13),
        ("concentrate", "focus attention", 2, 9), ("concept", "abstract idea", 2, 10),
        ("conclude", "bring to end", 2, 10), ("concurrent", "existing at same time", 4, 15),
        ("conduct", "organize and carry out", 2, 10), ("confer", "have discussions", 3, 13),
        ("confine", "restrict within limits", 3, 12), ("confirm", "establish truth of", 2, 9),
        ("conflict", "serious disagreement", 2, 10), ("conform", "comply with rules", 3, 12),
        ("consent", "permission for something", 2, 11), ("consequent", "following as result", 3, 13),
        ("considerable", "notably large in size", 2, 10), ("consist", "be composed of", 2, 10),
        ("constant", "occurring continuously", 2, 10), ("constitute", "be part of whole", 3, 12),
        ("constrain", "severely restrict scope", 3, 13), ("construct", "build by putting parts together", 2, 10),
        ("consult", "seek information from", 2, 10), ("consume", "use up", 2, 9),
        ("contact", "communicate with", 1, 8), ("contemporary", "belonging to present time", 3, 12),
        ("context", "circumstances forming setting", 2, 10), ("contract", "written agreement", 2, 10),
        ("contradict", "deny truth of", 3, 12), ("contrary", "opposite in nature", 3, 12),
        ("contrast", "compare to show differences", 2, 10), ("contribute", "give to help achieve", 2, 9),
        ("controversy", "prolonged public disagreement", 3, 13), ("convene", "come together for meeting", 3, 13),
        ("convention", "way things usually done", 3, 12), ("convert", "change form of", 2, 10),
        ("convince", "persuade to do something", 2, 10), ("cooperate", "work together", 2, 9),
        ("coordinate", "organize different elements", 3, 12), ("core", "central part", 2, 9),
        ("corporate", "relating to corporation", 3, 12), ("correspond", "have close similarity", 3, 12),
        ("couple", "two individuals together", 1, 7), ("create", "bring something into existence", 1, 7),
        ("credit", "acknowledgment of merit", 2, 10), ("criteria", "principles for judging", 3, 12),
        ("crucial", "extremely important", 2, 11), ("culture", "arts and customs", 1, 8),
        ("currency", "system of money", 2, 11), ("cycle", "series of events repeated", 2, 10),
        ("data", "facts and statistics", 2, 8), ("debate", "formal discussion", 2, 10),
        ("decade", "period of ten years", 2, 9), ("decline", "become smaller or worse", 2, 10),
        ("deduce", "arrive at conclusion", 3, 13), ("define", "state meaning exactly", 2, 9),
        ("definite", "clearly stated", 2, 9), ("demonstrate", "clearly show existence", 2, 10),
        ("denote", "be sign of", 3, 12), ("deny", "state that something is not true", 2, 9),
        ("depress", "make sad", 2, 10), ("derive", "obtain something from source", 3, 11),
        ("design", "plan and make", 1, 8), ("despite", "in spite of", 2, 10),
        ("detect", "discover presence of", 3, 11), ("deviate", "depart from established course", 4, 15),
        ("device", "piece of equipment", 2, 9), ("devote", "give time to", 2, 10),
        ("differentiate", "recognize difference", 3, 12), ("dimension", "measurable extent", 3, 11),
        ("diminish", "make smaller", 3, 12), ("discrete", "individually separate", 4, 15),
        ("discriminate", "recognize distinction", 3, 13), ("displace", "move from usual position", 3, 12),
        ("display", "put something on show", 2, 9), ("dispose", "get rid of", 2, 10),
        ("distinct", "recognizably different", 3, 11), ("distort", "give misleading account", 3, 13),
        ("distribute", "give shares to each", 2, 10), ("diverse", "showing great deal of variety", 2, 11),
        ("document", "written account", 2, 9), ("domain", "area of territory", 3, 12),
        ("domestic", "relating to home country", 2, 10), ("dominate", "have commanding influence", 2, 11),
        ("draft", "preliminary version", 2, 10), ("drama", "play for theatre", 1, 8),
        ("duration", "time during which something continues", 3, 11), ("dynamic", "characterized by constant change", 3, 12),
        ("economy", "wealth and resources of country", 2, 10), ("edit", "prepare written material", 2, 9),
        ("element", "essential part", 2, 9), ("eliminate", "completely remove", 2, 11),
        ("emerge", "move out of", 2, 11), ("emphasis", "special importance given", 3, 11),
        ("empirical", "based on observation", 4, 14), ("enable", "give ability to", 2, 10),
        ("encounter", "unexpectedly meet", 2, 11), ("energy", "power derived from resources", 1, 8),
        ("enforce", "compel observance of", 3, 12), ("enhance", "intensify or improve", 3, 11),
        ("enormous", "very large in size", 2, 10), ("ensure", "make certain that", 2, 10),
        ("entity", "thing with distinct existence", 3, 13), ("environment", "surroundings", 1, 8),
        ("episode", "event or group of events", 2, 11), ("equation", "mathematical statement", 3, 11),
        ("equip", "supply with necessary items", 2, 10), ("equivalent", "equal in value", 3, 11),
        ("erode", "gradually wear away", 3, 12), ("error", "mistake", 1, 8),
        ("establish", "set up on firm basis", 2, 10), ("estate", "extensive area of land", 2, 11),
        ("estimate", "roughly calculate", 2, 10), ("ethnic", "relating to population group", 3, 12),
        ("evaluate", "form idea of amount", 2, 10), ("eventual", "occurring at end of process", 3, 11),
        ("evident", "clearly seen or understood", 2, 10), ("evolve", "develop gradually", 3, 11),
        ("exceed", "be greater than", 2, 11), ("exclude", "deny access to", 2, 11),
        ("execute", "carry out or accomplish", 3, 12), ("exhibit", "publicly display", 2, 11),
        ("exist", "have objective reality", 1, 8), ("expand", "become larger", 2, 9),
        ("expert", "person with special knowledge", 2, 9), ("explicit", "stated clearly", 3, 12),
        ("exploit", "make full use of", 3, 12), ("expose", "reveal existence of", 2, 11),
        ("external", "belonging to outside", 2, 10), ("extract", "remove or take out", 3, 11),
        ("facilitate", "make action easier", 4, 14), ("factor", "circumstance contributing to result", 2, 10),
        ("feature", "distinctive attribute", 2, 9), ("federal", "relating to central government", 3, 12),
        ("fee", "payment made to professional", 2, 9), ("file", "collection of papers", 1, 8),
        ("final", "coming at end", 1, 7), ("finance", "management of money", 2, 10),
        ("finite", "having limits", 3, 13), ("flexible", "capable of bending", 2, 10),
        ("fluctuate", "rise and fall irregularly", 3, 13), ("focus", "center of interest", 2, 9),
        ("format", "way something is arranged", 2, 10), ("formula", "mathematical rule", 3, 11),
        ("forthcoming", "about to happen", 3, 12), ("foundation", "underlying basis", 2, 10),
        ("framework", "basic structure", 3, 11), ("function", "activity proper to person", 2, 9),
        ("fund", "sum of money saved", 2, 9), ("fundamental", "forming necessary base", 3, 11),
        ("furthermore", "in addition", 3, 12), ("gender", "state of being male or female", 2, 10),
        ("generate", "cause something to arise", 2, 11), ("generation", "all people born around same time", 2, 10),
        ("globe", "earth", 1, 8), ("goal", "object of ambition", 1, 8),
        ("grade", "particular level of rank", 2, 9), ("grant", "agree to give", 2, 10),
        ("guarantee", "formal promise", 2, 11), ("guideline", "general rule or advice", 3, 11),
        ("hence", "as consequence", 3, 12), ("hierarchy", "ranking system", 4, 14),
        ("highlight", "emphasize", 2, 10), ("hypothesis", "supposition explanation", 4, 13),
        ("identical", "similar in every detail", 2, 10), ("identify", "establish who someone is", 2, 9),
        ("ideology", "system of ideas", 4, 15), ("ignorant", "lacking knowledge", 2, 11),
        ("illustrate", "explain by examples", 2, 10), ("image", "representation of form", 1, 8),
        ("imply", "strongly suggest truth", 3, 11), ("impose", "force acceptance of", 3, 12),
        ("incentive", "thing that motivates", 3, 12), ("incidence", "occurrence or rate", 3, 13),
        ("incline", "lean or slope away", 2, 11), ("income", "money received regularly", 2, 9),
        ("incorporate", "take in as part of whole", 3, 12), ("index", "alphabetical list", 3, 11),
        ("indicate", "point out", 2, 9), ("individual", "single human being", 2, 9),
        ("induce", "succeed in persuading", 3, 13), ("inevitable", "certain to happen", 3, 12),
        ("infer", "deduce from evidence", 3, 12), ("infrastructure", "basic facilities", 4, 14),
        ("inherent", "existing as natural part", 4, 14), ("inhibit", "hinder or restrain", 3, 13),
        ("initial", "existing at beginning", 2, 10), ("initiate", "cause process to begin", 3, 12),
        ("innovation", "new method or idea", 3, 11), ("input", "what is put into system", 2, 10),
        ("insert", "place or fit into", 2, 10), ("insight", "accurate understanding", 3, 12),
        ("inspect", "look at closely", 2, 10), ("instance", "example or occurrence", 2, 10),
        ("institute", "organization for promotion", 3, 11), ("instruct", "teach subject to", 2, 9),
        ("integral", "necessary to make complete", 4, 14), ("integrate", "combine to form whole", 3, 12),
        ("integrity", "quality of being honest", 3, 12), ("intelligence", "ability to acquire knowledge", 2, 10),
        ("intense", "having great strength", 2, 10), ("interact", "act reciprocally", 3, 11),
        ("intermediate", "coming between extremes", 3, 12), ("internal", "situated on inside", 2, 10),
        ("interpret", "explain meaning of", 2, 11), ("interval", "intervening time", 3, 11),
        ("intervene", "come between to prevent", 3, 12), ("intrinsic", "belonging naturally", 4, 15),
        ("invest", "expend money for profit", 2, 10), ("investigate", "carry out research", 2, 11),
        ("invoke", "cite as authority", 4, 15), ("involve", "include as necessary part", 2, 9),
        ("isolate", "place apart from others", 3, 11), ("issue", "important topic for debate", 2, 9),
        ("item", "individual article", 1, 8), ("job", "paid position of employment", 1, 7),
        ("journal", "newspaper or magazine", 2, 9), ("justify", "show to be right", 3, 11),
        ("label", "small piece of paper", 2, 9), ("labor", "work requiring physical effort", 2, 9),
        ("layer", "sheet or thickness", 2, 10), ("lecture", "educational talk", 2, 9),
        ("legal", "permitted by law", 2, 9), ("legislate", "make or enact laws", 4, 14),
        ("levy", "impose tax or fee", 3, 13), ("liberal", "willing to respect ideas", 3, 11),
        ("license", "permit from authority", 2, 10), ("likewise", "in same way", 3, 11),
        ("link", "relationship between things", 2, 9), ("locate", "discover exact place", 2, 9),
        ("logic", "reasoning conducted according to principles", 3, 11), ("maintain", "cause to continue", 2, 10),
        ("major", "important or significant", 2, 8), ("manipulate", "handle or control skillfully", 3, 13),
        ("manual", "handbook giving instructions", 2, 10), ("margin", "edge or border", 2, 10),
        ("mature", "fully developed", 2, 10), ("maximum", "greatest amount possible", 2, 10),
        ("mechanism", "system of parts working together", 3, 12), ("media", "main means of communication", 2, 9),
        ("medical", "relating to medicine", 1, 8), ("medium", "middle state between extremes", 2, 10),
        ("mental", "relating to mind", 2, 9), ("method", "particular procedure", 2, 9),
        ("migrate", "move from one region to another", 3, 11), ("military", "relating to armed forces", 2, 9),
        ("minimal", "smallest in amount", 3, 11), ("minimum", "least or smallest amount", 2, 10),
        ("minor", "lesser in importance", 2, 9), ("mode", "way something occurs", 3, 11),
        ("modify", "make partial changes to", 2, 11), ("monitor", "observe and check progress", 2, 10),
        ("motive", "reason for doing something", 3, 11), ("mutual", "experienced by both parties", 3, 11),
        ("negate", "nullify or make ineffective", 4, 14), ("network", "group or system of connections", 2, 10),
        ("neutral", "not supporting either side", 2, 11), ("nevertheless", "in spite of that", 3, 12),
        ("norm", "standard or pattern", 3, 11), ("normal", "conforming to standard", 1, 8),
        ("notion", "conception or belief", 3, 11), ("nuclear", "relating to nucleus of atom", 3, 11),
        ("objective", "not influenced by personal feelings", 3, 11), ("obtain", "get or acquire", 2, 9),
        ("obvious", "easily perceived or understood", 2, 9), ("occupy", "reside or have one's place", 2, 10),
        ("occur", "happen or take place", 2, 9), ("odd", "strange or unusual", 1, 8),
        ("offset", "counteract something", 3, 12), ("ongoing", "continuing to exist", 2, 10),
        ("option", "thing that is chosen", 2, 9), ("orient", "align relative to compass", 3, 12),
        ("outcome", "way thing turns out", 2, 10), ("output", "amount of something produced", 2, 10),
        ("overall", "taking everything into account", 2, 10), ("overlap", "extend over and cover", 3, 11),
        ("overseas", "in or to foreign country", 2, 10), ("panel", "flat or curved component", 2, 10),
        ("paradigm", "typical example or pattern", 4, 15), ("paragraph", "distinct section of writing", 2, 9),
        ("parallel", "side by side and same distance", 2, 10), ("parameter", "numerical characteristic", 4, 14),
        ("participate", "take part in", 2, 10), ("partner", "person who shares", 1, 8),
        ("passive", "accepting without resistance", 3, 11), ("perceive", "become aware through senses", 3, 11),
        ("percent", "rate or proportion per hundred", 2, 9), ("period", "length of time", 1, 8),
        ("persist", "continue firmly", 3, 11), ("perspective", "particular attitude toward something", 3, 11),
        ("phase", "distinct period or stage", 2, 10), ("phenomenon", "fact or situation observed", 4, 13),
        ("philosophy", "study of fundamental nature", 3, 12), ("physical", "relating to body", 1, 8),
        ("plus", "with addition of", 1, 7), ("policy", "course of action", 2, 9),
        ("portion", "part or share of whole", 2, 10), ("pose", "present or constitute", 2, 10),
        ("positive", "constructive or confident", 1, 8), ("potential", "having possibility of developing", 2, 10),
        ("practitioner", "person actively engaged in profession", 3, 12), ("precede", "come before in time", 3, 11),
        ("precise", "marked by exactness", 3, 11), ("predict", "say what will happen", 2, 10),
        ("preliminary", "preceding main part", 3, 12), ("presume", "suppose to be case", 3, 12),
        ("previous", "existing before in time", 2, 9), ("primary", "of chief importance", 2, 9),
        ("prime", "of first importance", 2, 10), ("principal", "first in order of importance", 3, 11),
        ("principle", "fundamental truth", 2, 10), ("prior", "existing before", 3, 11),
        ("priority", "fact of being regarded as important", 3, 11), ("proceed", "begin course of action", 2, 10),
        ("process", "series of actions", 1, 8), ("professional", "relating to profession", 2, 9),
        ("project", "individual or collaborative enterprise", 1, 8), ("promote", "support or encourage", 2, 10),
        ("proportion", "part or share of whole", 3, 11), ("prospect", "possibility that something will happen", 3, 11),
        ("protocol", "official procedure", 4, 14), ("psychology", "scientific study of mind", 3, 12),
        ("publication", "book or journal issued publicly", 3, 11), ("publish", "prepare and issue for sale", 2, 9),
        ("purchase", "acquire by paying money", 2, 9), ("pursue", "follow in order to catch", 2, 11),
        ("qualitative", "relating to quality", 4, 14), ("quote", "repeat words from", 2, 10),
        ("radical", "relating to fundamental nature", 3, 12), ("random", "made without conscious choice", 3, 11),
        ("range", "area of variation", 2, 9), ("ratio", "quantitative relation", 3, 12),
        ("rational", "based on reason", 3, 11), ("react", "respond to stimulus", 2, 10),
        ("recover", "return to normal state", 2, 10), ("refine", "remove impurities", 3, 11),
        ("region", "area of country", 1, 8), ("register", "put name on official list", 2, 9),
        ("regulate", "control rate of operation", 3, 11), ("reject", "dismiss as inadequate", 2, 10),
        ("relax", "make less tense", 1, 8), ("release", "allow to move freely", 2, 9),
        ("relevant", "closely connected to matter", 3, 11), ("reluctance", "unwillingness to do something", 3, 12),
        ("rely", "depend on with confidence", 2, 9), ("remove", "take away from position", 1, 8),
        ("require", "need for particular purpose", 2, 8), ("research", "investigation to establish facts", 2, 8),
        ("reside", "have one's permanent home", 3, 11), ("resolve", "settle dispute", 2, 11),
        ("resource", "stock that can be drawn on", 2, 9), ("respond", "say something in reply", 2, 9),
        ("restore", "bring back to former condition", 3, 11), ("restrict", "put limit on", 2, 10),
        ("retain", "continue to have", 3, 11), ("reveal", "make known to others", 2, 10),
        ("revenue", "income from business operations", 3, 12), ("reverse", "move backwards", 2, 10),
        ("revise", "reconsider and alter", 2, 11), ("revolution", "forcible overthrow of government", 2, 11),
        ("rigid", "unable to bend", 3, 11), ("role", "actor's part in play", 1, 8),
        ("route", "way taken to get somewhere", 2, 9), ("scenario", "written outline of play", 3, 12),
        ("schedule", "plan for carrying out process", 2, 9), ("scheme", "large-scale systematic plan", 3, 11),
        ("scope", "extent of area covered", 3, 11), ("section", "any of more or less distinct parts", 1, 8),
        ("sector", "area or portion distinct from others", 3, 11), ("secure", "fixed so as not to give way", 2, 10),
        ("seek", "attempt to find", 2, 9), ("select", "carefully choose as best", 2, 9),
        ("sequence", "particular order of related events", 3, 11), ("series", "number of things coming one after another", 2, 9),
        ("sex", "either of two main categories", 1, 8), ("shift", "move from one position to another", 2, 10),
        ("significant", "sufficiently great to be worthy of attention", 3, 10), ("similar", "resembling without being identical", 1, 8),
        ("simulate", "imitate appearance of", 4, 14), ("site", "area of ground on which town is built", 1, 8),
        ("so-called", "commonly designated by name or term specified", 3, 11), ("sole", "one and only", 3, 11),
        ("somewhat", "to moderate extent", 2, 10), ("source", "place from which something originates", 2, 9),
        ("specific", "clearly defined", 2, 9), ("specify", "identify clearly", 3, 11),
        ("sphere", "round solid figure", 2, 10), ("stable", "not likely to change", 2, 10),
        ("statistic", "fact or piece of data", 3, 11), ("status", "relative social position", 2, 10),
        ("straightforward", "uncomplicated and easy to do", 2, 11), ("strategy", "plan of action", 2, 10),
        ("stress", "pressure or tension", 2, 9), ("structure", "arrangement of parts", 2, 9),
        ("style", "manner of doing something", 1, 8), ("submit", "accept or yield to superior force", 2, 11),
        ("subordinate", "lower in rank", 4, 14), ("subsequent", "coming after something else", 3, 12),
        ("subsidy", "sum of money granted by government", 4, 14), ("substitute", "person or thing acting for another", 3, 11),
        ("subtle", "so delicate as to be difficult to analyze", 3, 12), ("succeed", "achieve desired aim", 2, 9),
        ("successive", "following one another in uninterrupted succession", 3, 12), ("sufficient", "enough to meet needs", 3, 11),
        ("sum", "particular amount of money", 1, 8), ("summary", "brief statement of main points", 2, 10),
        ("supplement", "thing added to complete", 3, 12), ("survey", "look closely at", 2, 10),
        ("survive", "continue to live", 2, 9), ("suspend", "temporarily prevent from continuing", 3, 12),
        ("sustain", "strengthen or support physically", 3, 11), ("symbol", "thing representing something else", 2, 10),
        ("tape", "narrow strip of material", 1, 8), ("target", "person or object aimed at", 2, 9),
        ("task", "piece of work to be done", 1, 7), ("team", "group of players forming one side", 1, 7),
        ("technical", "involving special skills", 2, 10), ("technique", "way of carrying out activity", 2, 10),
        ("technology", "application of scientific knowledge", 2, 9), ("temporary", "lasting for limited time", 2, 10),
        ("tense", "stretched tight", 2, 10), ("terminate", "bring to an end", 3, 12),
        ("text", "written or printed work", 1, 8), ("theme", "subject of talk", 2, 9),
        ("theory", "supposition explaining something", 2, 10), ("thereby", "by that means", 3, 12),
        ("thesis", "statement or theory advanced", 4, 14), ("topic", "subject of conversation", 2, 9),
        ("trace", "find or discover by investigation", 2, 10), ("track", "rough path", 1, 8),
        ("tradition", "transmission of customs", 2, 10), ("transfer", "move from one place to another", 2, 10),
        ("transform", "make thorough change in form", 3, 11), ("transit", "process of carrying", 3, 12),
        ("transmit", "cause to pass on", 3, 12), ("transport", "carry people or goods from one place to another", 2, 9),
        ("trend", "general direction of change", 2, 10), ("trigger", "cause event to happen", 3, 11),
        ("ultimate", "being the best achievable", 3, 12), ("undergo", "experience something unpleasant", 3, 11),
        ("underlie", "be the cause of", 4, 14), ("undertake", "commit oneself to begin", 3, 12),
        ("uniform", "remaining same in all cases", 3, 11), ("unify", "make into single coherent whole", 3, 12),
        ("unique", "being the only one of its kind", 2, 10), ("unity", "state of being united", 3, 11),
        ("universal", "affecting all people", 3, 11), ("update", "make more modern", 2, 10),
        ("upgrade", "raise to higher standard", 2, 11), ("utility", "state of being useful", 3, 12),
        ("utilize", "make practical use of", 3, 11), ("valid", "having sound basis in logic", 3, 11),
        ("vary", "change or cause to change", 2, 9), ("vehicle", "thing used for transporting", 2, 9),
        ("version", "particular form of something", 2, 10), ("via", "traveling through", 2, 10),
        ("violate", "break or fail to comply with", 3, 12), ("virtual", "almost or nearly as described", 3, 12),
        ("visible", "able to be seen", 2, 9), ("visual", "relating to seeing", 2, 10),
        ("volume", "amount of space occupied", 2, 9), ("voluntary", "done of one's own free will", 3, 11),
        ("welfare", "health and happiness", 3, 11), ("whereas", "in contrast or comparison with fact", 3, 12),
        ("whereby", "by which", 4, 14), ("widespread", "found over large area", 3, 11)
    ]

    # Process all academic words
    new_words = []
    week = 8
    for i, (word, definition, difficulty, base_week) in enumerate(academic_words):
        if word not in existing_words:
            contexts = [
                f"The {word} demonstrates important characteristics in this context.",
                f"Understanding {word} is crucial for academic success."
            ]
            related = ["academic", "scholarly", "educational"] if "academic" not in word else ["study", "research", "analysis"]

            entry = create_entry(word, definition, "ACADEMIC", difficulty, base_week, contexts, related)
            new_words.append(entry)
            existing_words.add(word)

    # Add the new words to the total
    all_vocab.extend(new_words)

    return all_vocab

if __name__ == "__main__":
    complete_vocab = generate_2000_plus_vocabulary()

    print(f"Total vocabulary: {len(complete_vocab)} words")

    # Category distribution
    categories = {}
    for entry in complete_vocab:
        cat = entry.get('category', 'UNKNOWN')
        categories[cat] = categories.get(cat, 0) + 1

    print(f"Category distribution: {categories}")

    # Save complete database
    with open("complete_vocabulary_2000_plus.json", "w", encoding="utf-8") as f:
        json.dump(complete_vocab, f, indent=2, ensure_ascii=False)

    print("Complete 2000+ vocabulary database saved!")