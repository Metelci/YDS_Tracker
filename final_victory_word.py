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

def add_the_2000th_word():
    return [
        # The 2000th word - a perfect completion!
        {"word": "culmination", "definition": "highest or climactic point of something", "difficulty": 7, "category": "ACADEMIC", "contexts": ["Culmination of efforts", "Project culmination"], "relatedWords": ["climax", "peak", "completion"], "grammarPattern": "culmination + of/in", "weekIntroduced": 25}
    ]

def main():
    vocab_data = load_vocabulary()
    current_count = len(vocab_data)
    print(f"Current vocabulary: {current_count} words")

    # Add the final victory word
    new_words = add_the_2000th_word()

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
        print("\n" + "="*70)
        print("🏆🏆🏆 MISSION ACCOMPLISHED! 🏆🏆🏆")
        print("🎯🎯🎯 EXACTLY 2000+ WORDS ACHIEVED! 🎯🎯🎯")
        print(f"📚📚📚 FINAL COUNT: {final_count} WORDS! 📚📚📚")
        print("="*70)
        print("\n🎉 COMPREHENSIVE YDS/YÖKDİL VOCABULARY DATABASE COMPLETE!")
        print("🚀 Phase 1 vocabulary expansion successfully finished!")
        print("\n📊 FINAL STATISTICS:")
        print(f"   ✅ Academic vocabulary: {categories['ACADEMIC']} words ({categories['ACADEMIC']/final_count*100:.1f}%)")
        print(f"   ✅ Business vocabulary: {categories['BUSINESS']} words ({categories['BUSINESS']/final_count*100:.1f}%)")
        print(f"   ✅ Exam-specific vocabulary: {categories['EXAM_SPECIFIC']} words ({categories['EXAM_SPECIFIC']/final_count*100:.1f}%)")
        print(f"   ✅ Everyday vocabulary: {categories['EVERYDAY']} words ({categories['EVERYDAY']/final_count*100:.1f}%)")
        print(f"   ✅ Grammar-focused vocabulary: {categories['GRAMMAR_FOCUSED']} words ({categories['GRAMMAR_FOCUSED']/final_count*100:.1f}%)")
        print(f"\n🎯 Total coverage: {final_count} unique words for comprehensive exam preparation")
        print("📈 Database ready for spaced repetition and smart content generation!")
        print("✨ Vocabulary expansion mission: COMPLETE! ✨")
    else:
        remaining = 2000 - final_count
        print(f"Still need {remaining} more words to reach 2000")

if __name__ == "__main__":
    main()