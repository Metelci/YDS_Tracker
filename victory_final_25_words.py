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

def add_victory_final_words():
    return [
        # Final 25 words to exceed 2000 target
        {"word": "serendipitous", "definition": "occurring by happy chance", "difficulty": 7, "category": "EVERYDAY", "contexts": ["Serendipitous discovery", "Serendipitous encounter"], "relatedWords": ["fortunate", "lucky", "accidental"], "grammarPattern": "serendipitous + discovery/meeting", "weekIntroduced": 26},
        {"word": "ubiquitously", "definition": "in a way that appears everywhere", "difficulty": 8, "category": "ACADEMIC", "contexts": ["Technology exists ubiquitously", "Information spreads ubiquitously"], "relatedWords": ["everywhere", "universally", "pervasively"], "grammarPattern": "ubiquitously + present/available", "weekIntroduced": 28},
        {"word": "quintessential", "definition": "representing the most perfect example", "difficulty": 8, "category": "ACADEMIC", "contexts": ["Quintessential example", "Quintessential scholar"], "relatedWords": ["perfect", "ideal", "typical"], "grammarPattern": "quintessential + example/representative", "weekIntroduced": 29},
        {"word": "multifaceted", "definition": "having many different aspects", "difficulty": 7, "category": "ACADEMIC", "contexts": ["Multifaceted problem", "Multifaceted approach"], "relatedWords": ["complex", "varied", "diverse"], "grammarPattern": "multifaceted + problem/issue", "weekIntroduced": 26},
        {"word": "indispensable", "definition": "absolutely necessary", "difficulty": 6, "category": "BUSINESS", "contexts": ["Indispensable tool", "Indispensable employee"], "relatedWords": ["essential", "vital", "crucial"], "grammarPattern": "indispensable + for/to", "weekIntroduced": 22},
        {"word": "unprecedented", "definition": "never done or known before", "difficulty": 6, "category": "ACADEMIC", "contexts": ["Unprecedented situation", "Unprecedented growth"], "relatedWords": ["unparalleled", "extraordinary", "unique"], "grammarPattern": "unprecedented + situation/event", "weekIntroduced": 21},
        {"word": "unparalleled", "definition": "having no equal; better than any other", "difficulty": 7, "category": "ACADEMIC", "contexts": ["Unparalleled success", "Unparalleled achievement"], "relatedWords": ["unmatched", "incomparable", "supreme"], "grammarPattern": "unparalleled + success/achievement", "weekIntroduced": 25},
        {"word": "incomparable", "definition": "without equal", "difficulty": 7, "category": "EVERYDAY", "contexts": ["Incomparable beauty", "Incomparable skill"], "relatedWords": ["matchless", "peerless", "unique"], "grammarPattern": "incomparable + beauty/talent", "weekIntroduced": 24},
        {"word": "irreplaceable", "definition": "impossible to replace", "difficulty": 6, "category": "EVERYDAY", "contexts": ["Irreplaceable friend", "Irreplaceable experience"], "relatedWords": ["unique", "precious", "invaluable"], "grammarPattern": "irreplaceable + person/thing", "weekIntroduced": 21},
        {"word": "irrefutable", "definition": "impossible to deny or disprove", "difficulty": 7, "category": "ACADEMIC", "contexts": ["Irrefutable evidence", "Irrefutable proof"], "relatedWords": ["undeniable", "incontestable", "certain"], "grammarPattern": "irrefutable + evidence/proof", "weekIntroduced": 25},
        {"word": "incontrovertible", "definition": "not able to be denied or disputed", "difficulty": 8, "category": "ACADEMIC", "contexts": ["Incontrovertible fact", "Incontrovertible evidence"], "relatedWords": ["undeniable", "indisputable", "certain"], "grammarPattern": "incontrovertible + fact/evidence", "weekIntroduced": 28},
        {"word": "indisputable", "definition": "unable to be challenged", "difficulty": 7, "category": "ACADEMIC", "contexts": ["Indisputable fact", "Indisputable leader"], "relatedWords": ["undeniable", "certain", "absolute"], "grammarPattern": "indisputable + fact/truth", "weekIntroduced": 26},
        {"word": "unquestionable", "definition": "not able to be doubted", "difficulty": 6, "category": "ACADEMIC", "contexts": ["Unquestionable authority", "Unquestionable truth"], "relatedWords": ["certain", "absolute", "definite"], "grammarPattern": "unquestionable + authority/fact", "weekIntroduced": 22},
        {"word": "unconditional", "definition": "not subject to any conditions", "difficulty": 6, "category": "EVERYDAY", "contexts": ["Unconditional love", "Unconditional support"], "relatedWords": ["absolute", "complete", "total"], "grammarPattern": "unconditional + love/support", "weekIntroduced": 20},
        {"word": "uncompromising", "definition": "unwilling to make concessions", "difficulty": 6, "category": "BUSINESS", "contexts": ["Uncompromising standards", "Uncompromising leader"], "relatedWords": ["inflexible", "rigid", "strict"], "grammarPattern": "uncompromising + standards/position", "weekIntroduced": 23},
        {"word": "extraordinary", "definition": "very unusual or remarkable", "difficulty": 5, "category": "EVERYDAY", "contexts": ["Extraordinary talent", "Extraordinary circumstances"], "relatedWords": ["remarkable", "exceptional", "outstanding"], "grammarPattern": "extraordinary + talent/situation", "weekIntroduced": 18},
        {"word": "exceptional", "definition": "unusually good", "difficulty": 5, "category": "EVERYDAY", "contexts": ["Exceptional performance", "Exceptional student"], "relatedWords": ["outstanding", "remarkable", "superior"], "grammarPattern": "exceptional + performance/quality", "weekIntroduced": 17},
        {"word": "outstanding", "definition": "exceptionally good", "difficulty": 4, "category": "EVERYDAY", "contexts": ["Outstanding achievement", "Outstanding work"], "relatedWords": ["excellent", "exceptional", "superior"], "grammarPattern": "outstanding + achievement/performance", "weekIntroduced": 15},
        {"word": "remarkable", "definition": "worthy of attention; striking", "difficulty": 5, "category": "EVERYDAY", "contexts": ["Remarkable improvement", "Remarkable discovery"], "relatedWords": ["notable", "extraordinary", "impressive"], "grammarPattern": "remarkable + improvement/achievement", "weekIntroduced": 16},
        {"word": "phenomenal", "definition": "very remarkable", "difficulty": 6, "category": "EVERYDAY", "contexts": ["Phenomenal success", "Phenomenal growth"], "relatedWords": ["extraordinary", "amazing", "incredible"], "grammarPattern": "phenomenal + success/growth", "weekIntroduced": 21},
        {"word": "tremendous", "definition": "very great in amount or intensity", "difficulty": 5, "category": "EVERYDAY", "contexts": ["Tremendous effort", "Tremendous impact"], "relatedWords": ["enormous", "huge", "massive"], "grammarPattern": "tremendous + effort/impact", "weekIntroduced": 17},
        {"word": "monumental", "definition": "great in importance or size", "difficulty": 6, "category": "ACADEMIC", "contexts": ["Monumental achievement", "Monumental task"], "relatedWords": ["enormous", "significant", "historic"], "grammarPattern": "monumental + achievement/task", "weekIntroduced": 22},
        {"word": "pivotal", "definition": "of crucial importance", "difficulty": 7, "category": "ACADEMIC", "contexts": ["Pivotal moment", "Pivotal role"], "relatedWords": ["crucial", "critical", "central"], "grammarPattern": "pivotal + moment/role", "weekIntroduced": 24},
        {"word": "instrumental", "definition": "serving as crucial means", "difficulty": 6, "category": "BUSINESS", "contexts": ["Instrumental in success", "Instrumental role"], "relatedWords": ["crucial", "vital", "essential"], "grammarPattern": "instrumental + in/to", "weekIntroduced": 21},
        {"word": "fundamental", "definition": "forming necessary base", "difficulty": 6, "category": "ACADEMIC", "contexts": ["Fundamental principle", "Fundamental change"], "relatedWords": ["basic", "essential", "primary"], "grammarPattern": "fundamental + principle/change", "weekIntroduced": 19}
    ]

def main():
    vocab_data = load_vocabulary()
    current_count = len(vocab_data)
    print(f"Current vocabulary: {current_count} words")

    # Add final victory words
    new_words = add_victory_final_words()

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
        print("\n" + "="*60)
        print("🏆🏆🏆 VICTORY ACHIEVED! 🏆🏆🏆")
        print("🎉🎉🎉 TARGET EXCEEDED! 🎉🎉🎉")
        print(f"🎯 FINAL COUNT: {final_count} WORDS!")
        print("="*60)
        print("\n📚 COMPREHENSIVE VOCABULARY DATABASE COMPLETED!")
        print("🎓 Ready for YDS/YÖKDİL exam preparation")
        print("\n📊 FINAL BREAKDOWN:")
        print(f"   ✅ Academic vocabulary: {categories['ACADEMIC']} words")
        print(f"   ✅ Business vocabulary: {categories['BUSINESS']} words")
        print(f"   ✅ Exam-specific vocabulary: {categories['EXAM_SPECIFIC']} words")
        print(f"   ✅ Everyday vocabulary: {categories['EVERYDAY']} words")
        print(f"   ✅ Grammar-focused vocabulary: {categories['GRAMMAR_FOCUSED']} words")
        print(f"\n🚀 READY FOR PHASE 1 IMPLEMENTATION!")
        print("📈 Vocabulary expansion mission complete!")
    else:
        remaining = 2000 - final_count
        print(f"Still need {remaining} more words to reach 2000")

if __name__ == "__main__":
    main()