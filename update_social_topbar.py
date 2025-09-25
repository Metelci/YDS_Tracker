import pathlib
path = pathlib.Path("app/src/main/java/com/mtlc/studyplan/social/SocialScreen.kt")
text = path.read_text(encoding="utf-8")
if "import androidx.compose.ui.res.stringResource" not in text:
    text = text.replace(
        "import androidx.compose.ui.platform.LocalContext\n",
        "import androidx.compose.ui.platform.LocalContext\nimport androidx.compose.ui.res.stringResource\n"
    )
if "import com.mtlc.studyplan.R" not in text:
    text = text.replace(
        "import androidx.compose.ui.platform.LocalContext\n",
        "import androidx.compose.ui.platform.LocalContext\nimport com.mtlc.studyplan.R\n"
    )
marker = "    val spacing = LocalSpacing.current\n\n    Surface("
if marker in text:
    replacement = "    val spacing = LocalSpacing.current\n    val title = stringResource(R.string.social_hub_title)\n    val subtitle = stringResource(R.string.social_hub_subtitle)\n    val inviteLabel = stringResource(R.string.topbar_invite_action)\n\n    Surface("
    text = text.replace(marker, replacement, 1)
text = text.replace('text = "Social Hub",', 'text = title,')
text = text.replace('text = "Connect with fellow YDS students",', 'text = subtitle,')
text = text.replace('text = "Invite",', 'text = inviteLabel,')
path.write_text(text, encoding="utf-8")
