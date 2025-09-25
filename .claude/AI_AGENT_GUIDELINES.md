# StudyPlan YDS Exam Tracker - AI Agent Guidelines

## 🤖 Quick Reference for AI Coding Agents

### CRITICAL: Design System Rules
1. **NEVER use direct colors**: No `text-white`, `bg-black`, etc.
2. **ALWAYS use HSL format** for custom colors in CSS variables
3. **USE semantic tokens** from `index.css` and `tailwind.config.ts`
4. **CREATE component variants** instead of inline styles
5. **FOLLOW the established patterns** in existing components

### Current Implementation Status

#### ✅ COMPLETED FEATURES
- **Home Dashboard**: Progress ring, exam countdown, task list, streak counter, celebration animations
- **Navigation**: Bottom tab navigation (5 tabs) with active states and Turkish labels
- **Task Management**: Categories, filtering, daily/weekly/custom tabs with difficulty levels
- **Progress Tracking**: Complete analytics system with AI insights (7D/30D/All Time/Performance/Insights)
- **Social System**: Privacy-first profile customization, username/avatar selection, weekly goals
- **Leaderboard**: Ranking system with current user highlighting and streak tracking  
- **Study Groups**: Community features with activity indicators and member counts
- **Friends System**: Online status tracking and progress sharing
- **Achievement System**: Fully implemented with rarity tiers and progress tracking
- **Settings Page**: Theme toggle, notifications, privacy controls, Turkish localization
- **Design System**: Complete HSL color palette with semantic tokens for dark/light modes
- **Animation System**: 30+ predefined animations, celebrations, breathing effects, micro-interactions

#### 🚧 READY FOR ENHANCEMENT
- **Data Persistence**: LocalStorage framework ready, needs cloud sync integration
- **PWA Features**: Service worker implementation for offline support and push notifications
- **Advanced AI**: Expand current AI analytics with predictive insights and personalized recommendations
- **Real-time Features**: Live multiplayer study sessions and real-time leaderboard updates
- **Voice Integration**: Speech recognition for pronunciation practice
- **Mock Exams**: Full YDS simulation system with timing and scoring
- **Export Features**: PDF reports and progress data export functionality

### Component Architecture Patterns

#### Standard Component Structure
```typescript
// Pattern to follow for all new components
interface ComponentProps {
  // Required props first
  required: string;
  
  // Optional props with defaults
  optional?: boolean;
  className?: string;
  
  // Event handlers last
  onChange?: (value: string) => void;
}

export function Component({ 
  required, 
  optional = true, 
  className,
  onChange 
}: ComponentProps) {
  // Local state
  const [state, setState] = useState();
  
  // Event handlers
  const handleEvent = () => {
    // Implementation
    onChange?.(newValue);
  };
  
  return (
    <Card className={cn("base-styles", className)}>
      {/* Component JSX */}
    </Card>
  );
}
```

#### Animation Integration
```typescript
// Always use predefined animation classes
className={cn(
  "base-styles",
  isActive && "animate-scale-in",
  isLoading && "breathing",
  className
)}

// For hover effects
className="hover-scale transition-all duration-200"

// For celebrations
{isCompleted && <div className="celebrate">🎉 Tamamlandı!</div>}
```

### Data Flow Patterns

#### State Management (Current)
```typescript
// App.tsx - Main navigation state
const [activeTab, setActiveTab] = useState('home');

// Component level state for UI interactions  
const [isLoading, setIsLoading] = useState(false);
const [selectedCategory, setSelectedCategory] = useState(null);

// Props drilling for data sharing (intentionally simple)
<Tasks 
  tasks={tasks}
  onTaskComplete={handleTaskComplete}
/>
```

#### Task Management Pattern
```typescript
// Standard task operations across components
interface Task {
  id: string;
  title: string;
  skillType: 'grammar' | 'reading' | 'listening' | 'vocabulary';
  points: number;
  estimatedTime: number;
  completed: boolean;
}

// Task completion handler (used in Home.tsx, Tasks.tsx)
const handleTaskComplete = (taskId: string) => {
  setTasks(prev => prev.map(task => 
    task.id === taskId 
      ? { ...task, completed: !task.completed }
      : task
  ));
  
  // Update related state (progress, streak, etc.)
  updateProgress();
  updateStreak();
};
```

### File Naming & Organization

#### Component Files
```typescript
// PascalCase for component files
src/components/ComponentName.tsx    // ✅ Correct
src/components/component-name.tsx   // ❌ Wrong

// Props interface naming
interface ComponentNameProps { }     // ✅ Correct
interface ComponentProps { }         // ❌ Too generic
```

#### CSS Class Naming
```typescript
// Use semantic design tokens
className="bg-primary text-primary-foreground"  // ✅ Correct
className="bg-blue-500 text-white"              // ❌ Wrong

// Component-specific variants
className="bg-primary-container"                // ✅ Surface containers
className="text-muted-foreground"              // ✅ Secondary text
```

### Testing & Quality Guidelines

#### Accessibility Requirements
```typescript
// Always include ARIA labels for interactive elements
<button 
  aria-label="Mark task as complete"
  className="task-complete-button"
>
  <CheckIcon />
</button>

// Proper heading hierarchy
<h1>Main Page Title</h1>
<h2>Section Title</h2>
<h3>Subsection Title</h3>

// Focus management
className="focus:outline-none focus:ring-2 focus:ring-primary"
```

#### Performance Considerations
```typescript
// Use React.memo for expensive components
export const ExpensiveComponent = React.memo(({ data }) => {
  // Component implementation
});

// Avoid inline object creation in render
// ❌ Wrong
style={{ marginTop: 10 }}

// ✅ Correct  
const styles = { marginTop: 10 };
// or better, use Tailwind classes
className="mt-2.5"
```

### Common Pitfalls to Avoid

#### 1. Color System Violations
```typescript
// ❌ NEVER do this
className="text-white bg-blue-500"
style={{ color: '#ffffff' }}

// ✅ ALWAYS do this
className="text-primary-foreground bg-primary"
className="text-card-foreground bg-card"
```

#### 2. Animation Overuse
```typescript
// ❌ Too many animations
className="animate-bounce animate-pulse animate-spin"

// ✅ Subtle, purposeful animations
className="breathing"  // For idle states
className="celebrate"  // For success moments
```

#### 3. State Management Anti-patterns
```typescript
// ❌ Avoid prop drilling for deep nesting
<App>
  <Header user={user} settings={settings} />
  <Main user={user} tasks={tasks} settings={settings} />
</App>

// ✅ Keep state close to where it's used
function TaskSection() {
  const [tasks, setTasks] = useState([]);
  return <TaskList tasks={tasks} onUpdate={setTasks} />;
}
```

### Turkish Language Support

#### Text Content
```typescript
// All user-facing text in Turkish
const labels = {
  home: "Ana Sayfa",
  tasks: "Görevler", 
  progress: "İlerleme",
  settings: "Ayarlar",
  completed: "Tamamlandı",
  streak: "Seri"
};

// Date formatting for Turkish locale
const date = new Date().toLocaleDateString('tr-TR');
```

### Future Enhancement Patterns

#### Achievement System (Ready to implement)
```typescript
interface Achievement {
  id: string;
  title: string;
  description: string;
  tier: 'bronze' | 'silver' | 'gold' | 'platinum';
  points: number;
  icon: LucideIcon;
  unlocked: boolean;
  progress?: number;
}

// Achievement unlock pattern
const checkAchievements = (userStats: UserStats) => {
  achievements.forEach(achievement => {
    if (!achievement.unlocked && meetsRequirements(achievement, userStats)) {
      unlockAchievement(achievement.id);
      showCelebration(achievement);
    }
  });
};
```

#### Data Persistence (Next priority)
```typescript
// LocalStorage wrapper for persistence
const usePersistedState = <T>(key: string, defaultValue: T) => {
  const [state, setState] = useState<T>(() => {
    const stored = localStorage.getItem(key);
    return stored ? JSON.parse(stored) : defaultValue;
  });
  
  useEffect(() => {
    localStorage.setItem(key, JSON.stringify(state));
  }, [key, state]);
  
  return [state, setState] as const;
};
```

---

## 📋 Checklist for New Features

Before implementing any new feature:

- [ ] Does it use the design system tokens?
- [ ] Are colors in HSL format?
- [ ] Is it accessible (ARIA labels, keyboard nav)?
- [ ] Does it follow the established component patterns?
- [ ] Is the text in Turkish?
- [ ] Are animations subtle and purposeful?
- [ ] Is state management kept simple?
- [ ] Does it work in both light and dark themes?

---

*This document is optimized for AI coding agents working on the StudyPlan YDS Exam Tracker project.*
