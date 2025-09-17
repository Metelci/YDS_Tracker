package com.mtlc.studyplan.social

import androidx.annotation.StringRes
import com.mtlc.studyplan.R

/**
 * Five-tab navigation used by the Social Hub. Kept as a sealed class so we can
 * attach metadata (icons/strings) without scattering constants.
 */
enum class SocialTab(@StringRes val labelRes: Int) {
    Profile(R.string.social_tab_profile),
    Ranks(R.string.social_tab_ranks),
    Groups(R.string.social_tab_groups),
    Friends(R.string.social_tab_friends),
    Awards(R.string.social_tab_awards)
}

val SOCIAL_TABS = listOf(
    SocialTab.Profile,
    SocialTab.Ranks,
    SocialTab.Groups,
    SocialTab.Friends,
    SocialTab.Awards
)
