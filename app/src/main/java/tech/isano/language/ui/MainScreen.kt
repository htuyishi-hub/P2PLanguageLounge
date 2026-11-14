package tech.isano.language.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tech.isano.language.lounge.LoungeUiState
import tech.isano.language.lounge.LoungeViewModel
import tech.isano.language.ui.academy.LearnerDashboard
import tech.isano.language.ui.academy.TeacherDashboard
import tech.isano.language.ui.chat.ChatTab
import tech.isano.language.ui.chat.CommunityChatTab
import tech.isano.language.ui.lounge.LiveLoungeTab
import tech.isano.language.ui.lounge.LiveLoungeTabContent
import tech.isano.language.ui.theme.ElevatedNavy
import tech.isano.language.ui.theme.MutedSlateGrey
import tech.isano.language.ui.theme.ObsidianNavy
import tech.isano.language.ui.theme.PureWhite
import tech.isano.language.ui.theme.WarmCoral

enum class NavTab(val icon: ImageVector, val label: String) {
    ACADEMY(Icons.Default.School, "Academy"),
    COMMUNITY(Icons.Default.Mic, "Chat"),
    LOUNGE(Icons.Default.Videocam, "Live Lounge"),
    PROFILE(Icons.Default.AccountCircle, "Profile");

    companion object {
        val LEARN get() = ACADEMY
        val TALK get() = LOUNGE
        val CHAT get() = COMMUNITY
    }
}

enum class AcademyView {
    ROLE_SPLIT,
    TEACHER,
    LEARNER
}

/**
 * Returns progressively unlocked navigation tabs based on user level.
 * Level 1: LEARN (Academy), PROFILE
 * Level 2: LEARN (Academy), CHAT (Community), PROFILE
 * Level 3+: LEARN, CHAT, TALK (Live Lounge), PROFILE
 */
fun getUnlockedTabs(level: Int): List<NavTab> {
    return when {
        level <= 1 -> listOf(NavTab.ACADEMY, NavTab.PROFILE)
        level == 2 -> listOf(NavTab.ACADEMY, NavTab.COMMUNITY, NavTab.PROFILE)
        else -> listOf(NavTab.ACADEMY, NavTab.COMMUNITY, NavTab.LOUNGE, NavTab.PROFILE)
    }
}

@Composable
fun MainScreen(
    initialTab: NavTab = NavTab.ACADEMY,
    loungeViewModel: LoungeViewModel? = null,
    onNavigateToCanvas: () -> Unit = {},
    onNavigateToTeacherClass: () -> Unit = onNavigateToCanvas
) {
    // 1. Mock progression state (to be wired to Room DB)
    val userLevel = remember { mutableIntStateOf(1) }
    val unlockedTabs = remember(userLevel.intValue) { getUnlockedTabs(userLevel.intValue) }

    var selectedTab by remember { mutableStateOf(initialTab) }
    var academyView by remember { mutableStateOf(AcademyView.ROLE_SPLIT) }
    val loungeUiState = loungeViewModel?.uiState?.collectAsState()?.value ?: LoungeUiState.Idle

    // Keep selected tab valid if level changes
    LaunchedEffect(unlockedTabs) {
        if (selectedTab !in unlockedTabs) {
            selectedTab = unlockedTabs.first()
        }
    }

    Scaffold(
        containerColor = ObsidianNavy,
        bottomBar = {
            FourPillarsBottomBar(
                selectedTab = selectedTab,
                unlockedTabs = unlockedTabs,
                onTabSelected = {
                    selectedTab = it
                    if (it == NavTab.ACADEMY) {
                        academyView = AcademyView.ROLE_SPLIT
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(ObsidianNavy)
        ) {
            when (selectedTab) {
                NavTab.LOUNGE -> {
                    if (loungeViewModel != null) {
                        LiveLoungeTab(
                            viewModel = loungeViewModel,
                            onOpenCanvas = onNavigateToCanvas
                        )
                    } else {
                        LiveLoungeTabContent(
                            uiState = loungeUiState,
                            onJoinLobby = {},
                            onLeaveLobby = {},
                            onOpenCanvas = onNavigateToCanvas
                        )
                    }
                }
                NavTab.COMMUNITY -> CommunityChatTab()
                NavTab.ACADEMY -> {
                    when (academyView) {
                        AcademyView.ROLE_SPLIT -> {
                            AcademyTab(
                                onTeachClick = { academyView = AcademyView.TEACHER },
                                onLearnClick = { academyView = AcademyView.LEARNER }
                            )
                        }
                        AcademyView.TEACHER -> {
                            TeacherDashboard(
                                onStartLiveClass = onNavigateToTeacherClass,
                                onNavigateBack = { academyView = AcademyView.ROLE_SPLIT }
                            )
                        }
                        AcademyView.LEARNER -> {
                            LearnerDashboard(
                                onJoinLiveClass = onNavigateToCanvas,
                                onNavigateBack = { academyView = AcademyView.ROLE_SPLIT }
                            )
                        }
                    }
                }
                NavTab.PROFILE -> ProfileGoogleOneTapTab(
                    userLevel = userLevel.intValue,
                    onLevelUp = {
                        userLevel.intValue = if (userLevel.intValue >= 3) 1 else userLevel.intValue + 1
                    },
                    onRecordIntroClick = { /* Handle audio bio recording */ },
                    onBackupAccountClick = { /* Handle Google One-Tap backup */ }
                )
            }
        }
    }
}

/**
 * Bottom Bar with progressive disclosure.
 * Distributes visible tabs evenly (50% each for 2 tabs, 33.3% each for 3 tabs, 25% each for 4 tabs).
 * Wraps each item in AnimatedVisibility to slide in as a visual progression reward.
 */
@Composable
fun FourPillarsBottomBar(
    selectedTab: NavTab,
    unlockedTabs: List<NavTab>,
    onTabSelected: (NavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(ElevatedNavy)
            .navigationBarsPadding()
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            unlockedTabs.forEach { tab ->
                key(tab) {
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(tween(400)) + expandHorizontally(
                            animationSpec = tween(400, easing = FastOutSlowInEasing),
                            expandFrom = Alignment.CenterHorizontally
                        ),
                        exit = fadeOut(tween(300)) + shrinkHorizontally(
                            animationSpec = tween(300, easing = FastOutSlowInEasing),
                            shrinkTowards = Alignment.CenterHorizontally
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        PillarTabItem(
                            tab = tab,
                            isSelected = tab == selectedTab,
                            onClick = { onTabSelected(tab) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PillarTabItem(
    tab: NavTab,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(if (isSelected) WarmCoral else ElevatedNavy),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = tab.icon,
                contentDescription = tab.label,
                tint = if (isSelected) PureWhite else MutedSlateGrey,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = tab.label,
            color = if (isSelected) PureWhite else MutedSlateGrey,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

// -------------------------------------------------------------------------
// Tab: Academy (The Role Split: Teacher Zone vs. Learner Zone)
// -------------------------------------------------------------------------

@Composable
fun AcademyTab(
    onTeachClick: () -> Unit = {},
    onLearnClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianNavy)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            onClick = onTeachClick,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = ElevatedNavy),
            border = BorderStroke(4.dp, WarmCoral)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "TEACH",
                    tint = PureWhite,
                    modifier = Modifier.size(80.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "TEACH",
                    color = PureWhite,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }
        }

        Card(
            onClick = onLearnClick,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = ElevatedNavy)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MenuBook,
                    contentDescription = "LEARN & REVIEW",
                    tint = WarmCoral,
                    modifier = Modifier.size(80.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "LEARN & REVIEW",
                    color = PureWhite,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

// -------------------------------------------------------------------------
// Tab: Profile (Visual Identity, Audio Bio Recorder, Google One-Tap Backup)
// -------------------------------------------------------------------------

@Composable
fun ProfileGoogleOneTapTab(
    userLevel: Int = 1,
    onLevelUp: () -> Unit = {},
    onRecordIntroClick: () -> Unit = {},
    onBackupAccountClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianNavy)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        // Top Element: Circular Avatar Placeholder (120dp, ElevatedNavy)
        // Hidden testing trigger: double-tap avatar increments userLevel
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(ElevatedNavy)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { onLevelUp() }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = "Profile Avatar",
                tint = MutedSlateGrey,
                modifier = Modifier.size(90.dp)
            )

            // Visual Level Indicator Badge on Avatar
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(WarmCoral),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "L$userLevel",
                    color = PureWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        // Middle Element: Massive Screen-Wide Audio Bio Button (Height 100dp, WarmCoral)
        Button(
            onClick = onRecordIntroClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = WarmCoral)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = null,
                    tint = PureWhite,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "RECORD INTRO",
                    color = PureWhite,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Bottom Element: Standard-Height Google Backup Button (PureWhite, ObsidianNavy text/icon)
        Button(
            onClick = onBackupAccountClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PureWhite)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CloudSync,
                    contentDescription = null,
                    tint = ObsidianNavy,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "BACKUP ACCOUNT",
                    color = ObsidianNavy,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
