import pathlib
path = pathlib.Path('app/src/main/java/com/mtlc/studyplan/ui/components/StudyPlanTopBar.kt')
text = path.read_text(encoding='utf-8')
# add imports if missing
if 'import androidx.compose.ui.res.stringResource' not in text:
    text = text.replace('import androidx.compose.ui.platform.LocalContext\n', 'import androidx.compose.ui.platform.LocalContext\nimport androidx.compose.ui.res.stringResource\n')
if 'import com.mtlc.studyplan.R' not in text:
    text = text.replace('import androidx.compose.ui.unit.sp\n', 'import androidx.compose.ui.unit.sp\nimport com.mtlc.studyplan.R\n')
# remove Normalizer import
text = text.replace('import java.text.Normalizer\n', '')
# update TasksHeaderTopBar signature and body
old_block = "fun TasksHeaderTopBar(\n    title: String = \"Daily Tasks\",\n    subtitle: String = \"Complete tasks to build your streak\",\n    xpText: String = \"1,250 XP\",\n    modifier: Modifier = Modifier\n) {"
if old_block in text:
    new_block = "fun TasksHeaderTopBar(\n    title: String? = null,\n    subtitle: String? = null,\n    xpText: String = \"1,250 XP\",\n    modifier: Modifier = Modifier\n) {"
    text = text.replace(old_block, new_block)
# insert resolvedTitle/resolvedSubtitle
text = text.replace('    val appearance = rememberTopBarAppearance(StudyPlanTopBarStyle.Tasks)\n', '    val resolvedTitle = title ?: stringResource(R.string.topbar_tasks_title)\n    val resolvedSubtitle = subtitle ?: stringResource(R.string.topbar_tasks_subtitle)\n    val appearance = rememberTopBarAppearance(StudyPlanTopBarStyle.Tasks)\n')
text = text.replace('                        Text(\n                            text = title,\n', '                        Text(\n                            text = resolvedTitle,\n')
text = text.replace('                        Text(\n                            text = subtitle,\n', '                        Text(\n                            text = resolvedSubtitle,\n')
path.write_text(text, encoding='utf-8')
