package com.monager.app.ui.components

import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Lucide (ISC license, lucide.dev) stroke icons as in-code vectors: no PNG, no font, ~0 bytes of bitmap RAM.
 * Each ImageVector is built lazily once and cached.
 */
enum class MIcon(private val path: String) {
    Home("M15 21v-8a1 1 0 0 0-1-1h-4a1 1 0 0 0-1 1v8M3 10a2 2 0 0 1 .709-1.528l7-6a2 2 0 0 1 2.582 0l7 6A2 2 0 0 1 21 10v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"),
    Chart("M5 21v-6M12 21V3M19 21V9"),
    Wallet("M19 7V4a1 1 0 0 0-1-1H5a2 2 0 0 0 0 4h15a1 1 0 0 1 1 1v4h-3a2 2 0 0 0 0 4h3a1 1 0 0 0 1-1v-2a1 1 0 0 0-1-1M3 5v14a2 2 0 0 0 2 2h15a1 1 0 0 0 1-1v-4"),
    Users("M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2M16 3.128a4 4 0 0 1 0 7.744M22 21v-2a4 4 0 0 0-3-3.87M5 7a4 4 0 1 0 8 0a4 4 0 1 0 -8 0"),
    Plus("M5 12h14M12 5v14"),
    Search("M0 0m21 21-4.34-4.34M3 11a8 8 0 1 0 16 0a8 8 0 1 0 -16 0"),
    Calendar("M8 2v3M16 2v3M5 3h14a2 2 0 0 1 2 2v14a2 2 0 0 1 -2 2h-14a2 2 0 0 1 -2 -2v-14a2 2 0 0 1 2 -2ZM3 9h18"),
    ChevronDown("M0 0m6 9 6 6 6-6"),
    ArrowUp("M0 0m5 12 7-7 7 7M12 19V5"),
    ArrowDown("M12 5v14M0 0m19 12-7 7-7-7"),
    Transfer("M0 0m16 3 4 4-4 4M20 7H4M0 0m8 21-4-4 4-4M4 17h16"),
    Utensils("M3 2v7c0 1.1.9 2 2 2h4a2 2 0 0 0 2-2V2M7 2v20M21 15V2a5 5 0 0 0-5 5v6c0 1.1.9 2 2 2h3Zm0 0v7"),
    Car("M0 0m21 8-2 2-1.5-3.7A2 2 0 0 0 15.646 5H8.4a2 2 0 0 0-1.903 1.257L5 10 3 8M7 14h.01M17 14h.01M5 10h14a2 2 0 0 1 2 2v4a2 2 0 0 1 -2 2h-14a2 2 0 0 1 -2 -2v-4a2 2 0 0 1 2 -2ZM5 18v2M19 18v2"),
    ShoppingBag("M16 10a4 4 0 0 1-8 0M3.103 6.034h17.794M3.4 5.467a2 2 0 0 0-.4 1.2V20a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6.667a2 2 0 0 0-.4-1.2l-2-2.667A2 2 0 0 0 17 2H7a2 2 0 0 0-1.6.8z"),
    Receipt("M12 17V7M16 8h-6a2 2 0 0 0 0 4h4a2 2 0 0 1 0 4H8M4 3a1 1 0 0 1 1-1 1.3 1.3 0 0 1 .7.2l.933.6a1.3 1.3 0 0 0 1.4 0l.934-.6a1.3 1.3 0 0 1 1.4 0l.933.6a1.3 1.3 0 0 0 1.4 0l.933-.6a1.3 1.3 0 0 1 1.4 0l.934.6a1.3 1.3 0 0 0 1.4 0l.933-.6A1.3 1.3 0 0 1 19 2a1 1 0 0 1 1 1v18a1 1 0 0 1-1 1 1.3 1.3 0 0 1-.7-.2l-.933-.6a1.3 1.3 0 0 0-1.4 0l-.934.6a1.3 1.3 0 0 1-1.4 0l-.933-.6a1.3 1.3 0 0 0-1.4 0l-.933.6a1.3 1.3 0 0 1-1.4 0l-.934-.6a1.3 1.3 0 0 0-1.4 0l-.933.6a1.3 1.3 0 0 1-.7.2 1 1 0 0 1-1-1z"),
    More("M11 12a1 1 0 1 0 2 0a1 1 0 1 0 -2 0M18 12a1 1 0 1 0 2 0a1 1 0 1 0 -2 0M4 12a1 1 0 1 0 2 0a1 1 0 1 0 -2 0"),
    Settings("M9.671 4.136a2.34 2.34 0 0 1 4.659 0 2.34 2.34 0 0 0 3.319 1.915 2.34 2.34 0 0 1 2.33 4.033 2.34 2.34 0 0 0 0 3.831 2.34 2.34 0 0 1-2.33 4.033 2.34 2.34 0 0 0-3.319 1.915 2.34 2.34 0 0 1-4.659 0 2.34 2.34 0 0 0-3.32-1.915 2.34 2.34 0 0 1-2.33-4.033 2.34 2.34 0 0 0 0-3.831A2.34 2.34 0 0 1 6.35 6.051a2.34 2.34 0 0 0 3.319-1.915M9 12a3 3 0 1 0 6 0a3 3 0 1 0 -6 0"),
    Back("M0 0m15 18-6-6 6-6"),
    Sun("M8 12a4 4 0 1 0 8 0a4 4 0 1 0 -8 0M12 2v2M12 20v2M0 0m4.93 4.93 1.41 1.41M0 0m17.66 17.66 1.41 1.41M2 12h2M20 12h2M0 0m6.34 17.66-1.41 1.41M0 0m19.07 4.93-1.41 1.41"),
    Moon("M20.985 12.486a9 9 0 1 1-9.473-9.472c.405-.022.617.46.402.803a6 6 0 0 0 8.268 8.268c.344-.215.825-.004.803.401"),
    Plane("M17.8 19.2 16 11l3.5-3.5C21 6 21.5 4 21 3c-1-.5-3 0-4.5 1.5L13 8 4.8 6.2c-.5-.1-.9.1-1.1.5l-.3.5c-.2.5-.1 1 .3 1.3L9 12l-2 3H4l-1 1 3 2 2 3 1-1v-3l3-2 3.5 5.3c.3.4.8.5 1.3.3l.5-.2c.4-.3.6-.7.5-1.2z"),
    Heart("M2 9.5a5.5 5.5 0 0 1 9.591-3.676.56.56 0 0 0 .818 0A5.49 5.49 0 0 1 22 9.5c0 2.29-1.5 4-3 5.5l-5.492 5.313a2 2 0 0 1-3 .019L5 15c-1.5-1.5-3-3.2-3-5.5"),
    Graduation("M21.42 10.922a1 1 0 0 0-.019-1.838L12.83 5.18a2 2 0 0 0-1.66 0L2.6 9.08a1 1 0 0 0 0 1.832l8.57 3.908a2 2 0 0 0 1.66 0zM22 10v6M6 12.5V16a6 3 0 0 0 12 0v-3.5"),
    House("M15 21v-8a1 1 0 0 0-1-1h-4a1 1 0 0 0-1 1v8M3 10a2 2 0 0 1 .709-1.528l7-6a2 2 0 0 1 2.582 0l7 6A2 2 0 0 1 21 10v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"),
    Gift("M12 7v14M20 11v8a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2v-8M7.5 7a1 1 0 0 1 0-5A4.8 8 0 0 1 12 7a4.8 8 0 0 1 4.5-5 1 1 0 0 1 0 5M4 7h16a1 1 0 0 1 1 1v2a1 1 0 0 1 -1 1h-16a1 1 0 0 1 -1 -1v-2a1 1 0 0 1 1 -1Z"),
    Briefcase("M16 20V4a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16M4 6h16a2 2 0 0 1 2 2v10a2 2 0 0 1 -2 2h-16a2 2 0 0 1 -2 -2v-10a2 2 0 0 1 2 -2Z"),
    Edit("M21.174 6.812a1 1 0 0 0-3.986-3.987L3.842 16.174a2 2 0 0 0-.5.83l-1.321 4.352a.5.5 0 0 0 .623.622l4.353-1.32a2 2 0 0 0 .83-.497zM0 0m15 5 4 4"),
    Drag("M8 12a1 1 0 1 0 2 0a1 1 0 1 0 -2 0M8 5a1 1 0 1 0 2 0a1 1 0 1 0 -2 0M8 19a1 1 0 1 0 2 0a1 1 0 1 0 -2 0M14 12a1 1 0 1 0 2 0a1 1 0 1 0 -2 0M14 5a1 1 0 1 0 2 0a1 1 0 1 0 -2 0M14 19a1 1 0 1 0 2 0a1 1 0 1 0 -2 0"),
    ChevronRight("M0 0m9 18 6-6-6-6"),
    Check("M20 6 9 17l-5-5"),
    Close("M18 6 6 18M0 0m6 6 12 12"),
    Delete("M10 11v6M14 11v6M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6M3 6h18M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"),
    Coffee("M10 2v2M14 2v2M16 8a1 1 0 0 1 1 1v8a4 4 0 0 1-4 4H7a4 4 0 0 1-4-4V9a1 1 0 0 1 1-1h14a4 4 0 1 1 0 8h-1M6 2v2"),
    Phone("M7 2h10a2 2 0 0 1 2 2v16a2 2 0 0 1 -2 2h-10a2 2 0 0 1 -2 -2v-16a2 2 0 0 1 2 -2ZM12 18h.01"),
    Sparkles("M11.017 2.814a1 1 0 0 1 1.966 0l1.051 5.558a2 2 0 0 0 1.594 1.594l5.558 1.051a1 1 0 0 1 0 1.966l-5.558 1.051a2 2 0 0 0-1.594 1.594l-1.051 5.558a1 1 0 0 1-1.966 0l-1.051-5.558a2 2 0 0 0-1.594-1.594l-5.558-1.051a1 1 0 0 1 0-1.966l5.558-1.051a2 2 0 0 0 1.594-1.594zM20 2v4M22 4h-4M2 20a2 2 0 1 0 4 0a2 2 0 1 0 -4 0"),
    Eye("M2.062 12.348a1 1 0 0 1 0-.696 10.75 10.75 0 0 1 19.876 0 1 1 0 0 1 0 .696 10.75 10.75 0 0 1-19.876 0M9 12a3 3 0 1 0 6 0a3 3 0 1 0 -6 0"),
    EyeOff("M10.733 5.076a10.744 10.744 0 0 1 11.205 6.575 1 1 0 0 1 0 .696 10.747 10.747 0 0 1-1.444 2.49M14.084 14.158a3 3 0 0 1-4.242-4.242M17.479 17.499a10.75 10.75 0 0 1-15.417-5.151 1 1 0 0 1 0-.696 10.75 10.75 0 0 1 4.446-5.143M0 0m2 2 20 20"),
    User("M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2M8 7a4 4 0 1 0 8 0a4 4 0 1 0 -8 0"),
    Clock("M2 12a10 10 0 1 0 20 0a10 10 0 1 0 -20 0M12 6v6l4 2"),
    CreditCard("M4 5h16a2 2 0 0 1 2 2v10a2 2 0 0 1 -2 2h-16a2 2 0 0 1 -2 -2v-10a2 2 0 0 1 2 -2ZM2 10L22 10M6 14h2"),
    Landmark("M10 18v-7M11.119 2.205a2 2 0 0 1 1.762 0l7.84 3.846A.5.5 0 0 1 20.5 7h-17a.5.5 0 0 1-.22-.949zM14 18v-7M18 18v-7M3 22h18M6 18v-7"),
    Droplet("M12 22a7 7 0 0 0 7-7c0-2-1-3.9-3-5.5s-3.5-4-4-6.5c-.5 2.5-2 4.9-4 6.5C6 11.1 5 13 5 15a7 7 0 0 0 7 7z"),
    Lightbulb("M15 14c.2-1 .7-1.7 1.5-2.5 1-.9 1.5-2.2 1.5-3.5A6 6 0 0 0 6 8c0 1 .2 2.2 1.5 3.5.7.7 1.3 1.5 1.5 2.5M9 18h6M10 22h4"),
    Music("M21 16V3a1 1 0 0 0-1.28-.96l-10 2.917a1 1 0 0 0-.72.96V19M15 16a3 3 0 1 0 6 0a3 3 0 1 0 -6 0M3 19a3 3 0 1 0 6 0a3 3 0 1 0 -6 0"),
    Film("M0 0m12.296 3.464 3.02 3.956M20.2 6 3 11l-.9-2.4c-.3-1.1.3-2.2 1.3-2.5l13.5-4c1.1-.3 2.2.3 2.5 1.3zM3 11h18v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2zM0 0m6.18 5.276 3.1 3.899"),
    Gamepad("M6 11L10 11M8 9L8 13M15 12L15.01 12M18 10L18.01 10M17.32 5H6.68a4 4 0 0 0-3.978 3.59c-.006.052-.01.101-.017.152C2.604 9.416 2 14.456 2 16a3 3 0 0 0 3 3c1 0 1.5-.5 2-1l1.414-1.414A2 2 0 0 1 9.828 16h4.344a2 2 0 0 1 1.414.586L17 18c.5.5 1 1 2 1a3 3 0 0 0 3-3c0-1.545-.604-6.584-.685-7.258-.007-.05-.011-.1-.017-.151A4 4 0 0 0 17.32 5z"),
    Shirt("M20.38 3.46 16 2a4 4 0 0 1-8 0L3.62 3.46a2 2 0 0 0-1.34 2.23l.58 3.47a1 1 0 0 0 .99.84H6v10c0 1.1.9 2 2 2h8a2 2 0 0 0 2-2V10h2.15a1 1 0 0 0 .99-.84l.58-3.47a2 2 0 0 0-1.34-2.23z"),
    Book("M4 19.5v-15A2.5 2.5 0 0 1 6.5 2H19a1 1 0 0 1 1 1v18a1 1 0 0 1-1 1H6.5a1 1 0 0 1 0-5H20"),
    Tv("M0 0m17 2-5 5-5-5M4 7h16a2 2 0 0 1 2 2v11a2 2 0 0 1 -2 2h-16a2 2 0 0 1 -2 -2v-11a2 2 0 0 1 2 -2Z"),
    Wifi("M12 20h.01M2 8.82a15 15 0 0 1 20 0M5 12.859a10 10 0 0 1 14 0M8.5 16.429a5 5 0 0 1 7 0"),
    Pill("M0 0m10.5 20.5 10-10a4.95 4.95 0 1 0-7-7l-10 10a4.95 4.95 0 1 0 7 7ZM0 0m8.5 8.5 7 7"),
    Fuel("M14 13h2a2 2 0 0 1 2 2v2a2 2 0 0 0 4 0v-6.998a2 2 0 0 0-.59-1.42L18 5M14 21V5a2 2 0 0 0-2-2H5a2 2 0 0 0-2 2v16M2 21h13M3 9h11"),
    Bike("M15 17.5a3.5 3.5 0 1 0 7 0a3.5 3.5 0 1 0 -7 0M2 17.5a3.5 3.5 0 1 0 7 0a3.5 3.5 0 1 0 -7 0M14 5a1 1 0 1 0 2 0a1 1 0 1 0 -2 0M12 17.5V14l-3-3 4-3 2 3h2"),
    Bus("M8 6v6M15 6v6M2 12h19.6M18 18h3s.5-1.7.8-2.8c.1-.4.2-.8.2-1.2 0-.4-.1-.8-.2-1.2l-1.4-5C20.1 6.8 19.1 6 18 6H4a2 2 0 0 0-2 2v10h3M5 18a2 2 0 1 0 4 0a2 2 0 1 0 -4 0M9 18h5M14 18a2 2 0 1 0 4 0a2 2 0 1 0 -4 0"),
    Coins("M13.744 17.736a6 6 0 1 1-7.48-7.48M15 6h1v4M0 0m6.134 14.768.866-.5 2 3.464M10 8a6 6 0 1 0 12 0a6 6 0 1 0 -12 0"),
    TrendingUp("M16 7h6v6M0 0m22 7-8.5 8.5-5-5L2 17"),
    Cake("M16 13H3M16 17H3M0 0m7.2 7.9-3.388 2.5A2 2 0 0 0 3 12.01V20a1 1 0 0 0 1 1h16a1 1 0 0 0 1-1v-8.654c0-2-2.44-6.026-6.44-8.026a1 1 0 0 0-1.082.057L10.4 5.6M7 7a2 2 0 1 0 4 0a2 2 0 1 0 -4 0"),
    Scissors("M3 6a3 3 0 1 0 6 0a3 3 0 1 0 -6 0M8.12 8.12 12 12M20 4 8.12 15.88M3 18a3 3 0 1 0 6 0a3 3 0 1 0 -6 0M14.8 14.8 20 20"),
    Umbrella("M12 13v7a2 2 0 0 0 4 0M12 2v2M20.992 13a1 1 0 0 0 .97-1.274 10.284 10.284 0 0 0-19.923 0A1 1 0 0 0 3 13z"),
    Laptop("M18 5a2 2 0 0 1 2 2v8.526a2 2 0 0 0 .212.897l1.068 2.127a1 1 0 0 1-.9 1.45H3.62a1 1 0 0 1-.9-1.45l1.068-2.127A2 2 0 0 0 4 15.526V7a2 2 0 0 1 2-2zM20.054 15.987H3.946"),
    Cart("M0 0m2.05 2.05 1.099-.028a1 1 0 0 1 1.008.815l2.69 14.347A1 1 0 0 0 7.83 18H18M4.563 5h16.435a1 1 0 0 1 .981 1.204l-1.026 6.226A2 2 0 0 1 18.962 14H6.25M16 20a2 2 0 1 0 4 0a2 2 0 1 0 -4 0M6 20a2 2 0 1 0 4 0a2 2 0 1 0 -4 0"),
    Tag("M12.586 2.586A2 2 0 0 0 11.172 2H4a2 2 0 0 0-2 2v7.172a2 2 0 0 0 .586 1.414l8.704 8.704a2.426 2.426 0 0 0 3.42 0l6.58-6.58a2.426 2.426 0 0 0 0-3.42zM7 7.5a0.5 0.5 0 1 0 1 0a0.5 0.5 0 1 0 -1 0"),
    Star("M11.525 2.295a.53.53 0 0 1 .95 0l2.31 4.679a2.123 2.123 0 0 0 1.595 1.16l5.166.756a.53.53 0 0 1 .294.904l-3.736 3.638a2.123 2.123 0 0 0-.611 1.878l.882 5.14a.53.53 0 0 1-.771.56l-4.618-2.428a2.122 2.122 0 0 0-1.973 0L6.396 21.01a.53.53 0 0 1-.77-.56l.881-5.139a2.122 2.122 0 0 0-.611-1.879L2.16 9.795a.53.53 0 0 1 .294-.906l5.165-.755a2.122 2.122 0 0 0 1.597-1.16z"),
    Noodles("M11.248 3c.273.1.808.53.747 1.36-.05.83-.938 1.2-.989 2.02-.06.78.333 1.24.727 1.62M16.252 3c.268.1.794.53.745 1.36-.06.83-.923 1.2-.993 2.02-.05.78.338 1.24.725 1.62M19.5 12 22 6M4 12a1 1 0 00-.99 1.133A9 9 0 0012 21a9 9 0 008.99-7.867A1 1 0 0020 12zM6.252 3c.268.1.794.53.745 1.36-.06.83-.923 1.2-.993 2.02-.05.78.338 1.24.735 1.62M7 21h10"),
    Apple("M12 6.528V3a1 1 0 0 1 1-1h0M18.237 21A15 15 0 0 0 22 11a6 6 0 0 0-10-4.472A6 6 0 0 0 2 11a15.1 15.1 0 0 0 3.763 10 3 3 0 0 0 3.648.648 5.5 5.5 0 0 1 5.178 0A3 3 0 0 0 18.237 21"),
    Motorcycle("M0 0m18 14-1-3M0 0m3 9 6 2a2 2 0 0 1 2-2h2a2 2 0 0 1 1.99 1.81M8 17h3a1 1 0 0 0 1-1 6 6 0 0 1 6-6 1 1 0 0 0 1-1v-.75A5 5 0 0 0 17 5M16 17a3 3 0 1 0 6 0a3 3 0 1 0 -6 0M2 17a3 3 0 1 0 6 0a3 3 0 1 0 -6 0"),
    SprayBottle("M3 3h.01M7 5h.01M11 7h.01M3 7h.01M7 9h.01M3 11h.01M15 5h4v4h-4ZM0 0m19 9 2 2v10c0 .6-.4 1-1 1h-6c-.6 0-1-.4-1-1V11l2-2M0 0m13 14 8-2M0 0m13 19 8-2"),
    Dumbbell("M17.596 12.768a2 2 0 1 0 2.829-2.829l-1.768-1.767a2 2 0 0 0 2.828-2.829l-2.828-2.828a2 2 0 0 0-2.829 2.828l-1.767-1.768a2 2 0 1 0-2.829 2.829zM0 0m2.5 21.5 1.4-1.4M0 0m20.1 3.9 1.4-1.4M5.343 21.485a2 2 0 1 0 2.829-2.828l1.767 1.768a2 2 0 1 0 2.829-2.829l-6.364-6.364a2 2 0 1 0-2.829 2.829l1.768 1.767a2 2 0 0 0-2.828 2.829zM0 0m9.6 14.4 4.8-4.8"),
    Hospital("M12 7v4M14 21v-3a2 2 0 0 0-4 0v3M14 9h-4M18 11h2a2 2 0 0 1 2 2v6a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2v-9a2 2 0 0 1 2-2h2M18 21V5a2 2 0 0 0-2-2H8a2 2 0 0 0-2 2v16"),
    Ball("M11 7a16 16 20 0 1 10.98 4.362M12 12a13 13 0 0 1-8.66 5M16.83 13.634a16 16 0 0 1-9.267 7.328M20.66 17A13 13 0 0 0 12 12a13 13 0 0 1 0-10M8.17 15.366a16 16 0 0 1-1.713-11.69M2 12a10 10 0 1 0 20 0a10 10 0 1 0 -20 0"),
    Lipstick("M12 22a1 1 0 0 1 0-20 10 9 0 0 1 10 9 5 5 0 0 1-5 5h-2.25a1.75 1.75 0 0 0-1.4 2.8l.3.4a1.75 1.75 0 0 1-1.4 2.8zM13 6.5a0.5 0.5 0 1 0 1 0a0.5 0.5 0 1 0 -1 0M17 10.5a0.5 0.5 0 1 0 1 0a0.5 0.5 0 1 0 -1 0M6 12.5a0.5 0.5 0 1 0 1 0a0.5 0.5 0 1 0 -1 0M8 7.5a0.5 0.5 0 1 0 1 0a0.5 0.5 0 1 0 -1 0"),
    FileCheck("M6 22a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h8a2.4 2.4 0 0 1 1.704.706l3.588 3.588A2.4 2.4 0 0 1 20 8v12a2 2 0 0 1-2 2zM14 2v5a1 1 0 0 0 1 1h5M0 0m9 15 2 2 4-4"),
    MoneyOut("M12 18H4a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v5M18 12h.01M19 22v-6M0 0m22 19-3-3-3 3M6 12h.01M10 12a2 2 0 1 0 4 0a2 2 0 1 0 -4 0"),
    PiggyBank("M11 17h3v2a1 1 0 0 0 1 1h2a1 1 0 0 0 1-1v-3a3.16 3.16 0 0 0 2-2h1a1 1 0 0 0 1-1v-2a1 1 0 0 0-1-1h-1a5 5 0 0 0-2-4V3a4 4 0 0 0-3.2 1.6l-.3.4H11a6 6 0 0 0-6 6v1a5 5 0 0 0 2 4v3a1 1 0 0 0 1 1h2a1 1 0 0 0 1-1zM16 10h.01M2 8v1a2 2 0 0 0 2 2h1"),
    Building("M10 12h4M10 8h4M14 21v-3a2 2 0 0 0-4 0v3M6 10H4a2 2 0 0 0-2 2v7a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2V9a2 2 0 0 0-2-2h-2M6 21V5a2 2 0 0 1 2-2h8a2 2 0 0 1 2 2v16"),
    Key("M0 0m2 21 9.6-9.6M0 0m7.5 15.5 2.3 2.3a1 1 0 0 1 0 1.4l-2.1 2.1a1 1 0 0 1-1.4 0L4 19M10 7.5a5.5 5.5 0 1 0 11 0a5.5 5.5 0 1 0 -11 0"),
    Popcorn("M18 8a2 2 0 0 0 0-4 2 2 0 0 0-4 0 2 2 0 0 0-4 0 2 2 0 0 0-4 0 2 2 0 0 0 0 4M10 22 9 8M0 0m14 22 1-14M20 8c.5 0 .9.4.8 1l-2.6 12c-.1.5-.7 1-1.2 1H7c-.6 0-1.1-.4-1.2-1L3.2 9c-.1-.6.3-1 .8-1Z"),
    Cookie("M11 17h.01M11.496 2c.324-.016.558.292.529.615a4 4 0 004.235 4.368.713.713 0 01.758.757 4 4 0 004.366 4.237c.323-.03.63.204.614.527a10 10 0 01-2.915 6.566A1 1 0 114.93 4.918 10 10 0 0111.496 2M12 12h.01M16 16h.01M16 3h.01M21 4h.01M21 8h.01M7 14h.01M9 8h.01"),
    Landline("M13 2a9 9 0 0 1 9 9M13 6a5 5 0 0 1 5 5M13.832 16.568a1 1 0 0 0 1.213-.303l.355-.465A2 2 0 0 1 17 15h3a2 2 0 0 1 2 2v3a2 2 0 0 1-2 2A18 18 0 0 1 2 4a2 2 0 0 1 2-2h3a2 2 0 0 1 2 2v3a2 2 0 0 1-.8 1.6l-.468.351a1 1 0 0 0-.292 1.233 14 14 0 0 0 6.392 6.384"),
    Basket("M0 0m15 11-1 9M0 0m19 11-4-7M2 11h20M0 0m3.5 11 1.6 7.4a2 2 0 0 0 2 1.6h9.8a2 2 0 0 0 2-1.6l1.7-7.4M4.5 15.5h15M0 0m5 11 4-7M0 0m9 11 1 9"),
    Soap("M7.001 15.085A1.5 1.5 0 0 1 9 16.5M15 8.5a3.5 3.5 0 1 0 7 0a3.5 3.5 0 1 0 -7 0M2 16.5a5.5 5.5 0 1 0 11 0a5.5 5.5 0 1 0 -11 0M5 4.5a2.5 2.5 0 1 0 5 0a2.5 2.5 0 1 0 -5 0"),
    Dress("M2.048 18.566A2 2 0 0 0 4 21h16a2 2 0 0 0 1.952-2.434l-2-9A2 2 0 0 0 18 8H6a2 2 0 0 0-1.952 1.566zM8 11V6a4 4 0 0 1 8 0v5"),
    Jeans("M2 15a4 4 0 1 0 8 0a4 4 0 1 0 -8 0M14 15a4 4 0 1 0 8 0a4 4 0 1 0 -8 0M14 15a2 2 0 0 0-2-2 2 2 0 0 0-2 2M2.5 13 5 7c.7-1.3 1.4-2 3-2M21.5 13 19 7c-.7-1.3-1.5-2-3-2"),
    HourlyPay("M5 22h14M5 2h14M17 22v-4.172a2 2 0 0 0-.586-1.414L12 12l-4.414 4.414A2 2 0 0 0 7 17.828V22M7 2v4.172a2 2 0 0 0 .586 1.414L12 12l4.414-4.414A2 2 0 0 0 17 6.172V2"),
    Trophy("M10 14.66V17a1 1 0 0 1-1 1 2 2 0 0 0-2 2v2M14 14.66V17a1 1 0 0 0 1 1 2 2 0 0 1 2 2v2M17.916 10H19.5A2.5 2.5 0 0 0 22 7.5V5a1 1 0 0 0-1-1h-3M4 22h16M6 9a6 6 0 0 0 12 0V3a1 1 0 0 0-1-1H7a1 1 0 0 0-1 1zM6.084 10H4.5A2.5 2.5 0 0 1 2 7.5V5a1 1 0 0 1 1-1h3"),
    Candles("M9 5v4M8 9h2a1 1 0 0 1 1 1v4a1 1 0 0 1 -1 1h-2a1 1 0 0 1 -1 -1v-4a1 1 0 0 1 1 -1ZM9 15v2M17 3v2M16 5h2a1 1 0 0 1 1 1v6a1 1 0 0 1 -1 1h-2a1 1 0 0 1 -1 -1v-6a1 1 0 0 1 1 -1ZM17 13v3M3 3v16a2 2 0 0 0 2 2h16"),
    MoneyBag("M11 15h2a2 2 0 1 0 0-4h-3c-.6 0-1.1.2-1.4.6L3 17M0 0m7 21 1.6-1.4c.3-.4.8-.6 1.4-.6h4c1.1 0 2.1-.4 2.8-1.2l4.6-4.4a2 2 0 0 0-2.75-2.91l-4.2 3.9M0 0m2 16 6 6M13.1 9a2.9 2.9 0 1 0 5.8 0a2.9 2.9 0 1 0 -5.8 0M3 5a3 3 0 1 0 6 0a3 3 0 1 0 -6 0"),
    Banknote("M4 6h16a2 2 0 0 1 2 2v8a2 2 0 0 1 -2 2h-16a2 2 0 0 1 -2 -2v-8a2 2 0 0 1 2 -2ZM10 12a2 2 0 1 0 4 0a2 2 0 1 0 -4 0M6 12h.01M18 12h.01"),
    Wrench("M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.106-3.105c.32-.322.863-.22.983.218a6 6 0 0 1-8.259 7.057l-7.91 7.91a1 1 0 0 1-2.999-3l7.91-7.91a6 6 0 0 1 7.057-8.259c.438.12.54.662.219.984z"),
    Parking("M9 12h4a2 2 0 0 0 0-4H9.5a.5.5 0 0 0-.5.5V16M5 3h14a2 2 0 0 1 2 2v14a2 2 0 0 1 -2 2h-14a2 2 0 0 1 -2 -2v-14a2 2 0 0 1 2 -2Z"),
    Gem("M10.5 3 8 9l4 13 4-13-2.5-6M17 3a2 2 0 0 1 1.6.8l3 4a2 2 0 0 1 .013 2.382l-7.99 10.986a2 2 0 0 1-3.247 0l-7.99-10.986A2 2 0 0 1 2.4 7.8l2.998-3.997A2 2 0 0 1 7 3zM2 9h20"),
    Bed("M2 4v16M2 8h18a2 2 0 0 1 2 2v10M2 17h20M6 8v9"),
    CookingPot("M2 12h20M20 12v8a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2v-8M0 0m4 8 16-4M0 0m8.86 6.78-.45-1.81a2 2 0 0 1 1.45-2.43l1.94-.48a2 2 0 0 1 2.43 1.46l.45 1.8"),
    Waves("M2 12q2.5 2 5 0t5 0 5 0 5 0M2 19q2.5 2 5 0t5 0 5 0 5 0M2 5q2.5 2 5 0t5 0 5 0 5 0"),
    Billiard("M2 12a10 10 0 1 0 20 0a10 10 0 1 0 -20 0M6 12a6 6 0 1 0 12 0a6 6 0 1 0 -12 0M10 12a2 2 0 1 0 4 0a2 2 0 1 0 -4 0"),
    Shoe("M4 16v-2.38C4 11.5 2.97 10.5 3 8c.03-2.72 1.49-6 4.5-6C9.37 2 10 3.8 10 5.5c0 3.11-2 5.66-2 8.68V16a2 2 0 1 1-4 0ZM20 20v-2.38c0-2.12 1.03-3.12 1-5.62-.03-2.72-1.49-6-4.5-6C14.63 6 14 7.8 14 9.5c0 3.11 2 5.66 2 8.68V20a2 2 0 1 0 4 0ZM16 17h4M4 13h4"),
    Burger("M12 16H4a2 2 0 1 1 0-4h16a2 2 0 1 1 0 4h-4.25M5 12a2 2 0 0 1-2-2 9 7 0 0 1 18 0 2 2 0 0 1-2 2M5 16a2 2 0 0 0-2 2 3 3 0 0 0 3 3h12a3 3 0 0 0 3-3 2 2 0 0 0-2-2q0 0 0 0M0 0m6.67 12 6.13 4.6a2 2 0 0 0 2.8-.4l3.15-4.2"),
    Zap("M4 14a1 1 0 0 1-.78-1.63l9.9-10.2a.5.5 0 0 1 .86.46l-1.92 6.02A1 1 0 0 0 13 10h7a1 1 0 0 1 .78 1.63l-9.9 10.2a.5.5 0 0 1-.86-.46l1.92-6.02A1 1 0 0 0 11 14z"),
    Faucet("M4 12h9a3 3 0 0 1 3 3v1M4 9v6M16 19v1M12 12V8M9 8h6M16 22h.01"),
    Flame("M8.5 14.5A2.5 2.5 0 0 0 11 12c0-1.38-.5-2-1-3-1.072-2.143-.224-4.054 2-6 .5 2.5 2 4.9 4 6.5 2 1.6 3 3.5 3 5.5a7 7 0 1 1-14 0c0-1.153.433-2.294 1-3a2.5 2.5 0 0 0 2.5 2.5z"),
    Globe("M2 12a10 10 0 1 0 20 0a10 10 0 1 0 -20 0M12 2a14.5 14.5 0 0 0 0 20 14.5 14.5 0 0 0 0-20M2 12h20"),
    Cloud("M17.5 19H9a7 7 0 1 1 6.71-9h1.79a4.5 4.5 0 1 1 0 9Z"),
    Subscription("M0 0m17 2 4 4-4 4M3 11v-1a4 4 0 0 1 4-4h14M0 0m7 22-4-4 4-4M21 13v1a4 4 0 0 1-4 4H3"),
    Plug("M12 22v-5M9 8V2M15 8V2M18 8v5a4 4 0 0 1-4 4h-4a4 4 0 0 1-4-4V8Z"),
    Luggage("M6 20a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h12a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2M8 18V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v14M10 20h4M6 21a1 1 0 1 0 2 0a1 1 0 1 0 -2 0M16 21a1 1 0 1 0 2 0a1 1 0 1 0 -2 0"),
    PalmTree("M13 8c0-2.76-2.46-5-5.5-5S2 5.24 2 8h2l1-1 1 1h4M13 7.14A5.82 5.82 0 0 1 16.5 6c3.04 0 5.5 2.24 5.5 5h-3l-1-1-1 1h-3M5.89 9.71c-2.15 2.15-2.3 5.47-.35 7.43l4.24-4.25.7-.7.71-.71 2.12-2.12c-1.95-1.96-5.27-1.8-7.42.35M11 15.5c.5 2.5-.17 4.5-1 6.5h4c2-5.5-.5-12-1-14"),
    Hotel("M10 22v-6.57M12 11h.01M12 7h.01M14 15.43V22M15 16a5 5 0 0 0-6 0M16 11h.01M16 7h.01M8 11h.01M8 7h.01M6 2h12a2 2 0 0 1 2 2v16a2 2 0 0 1 -2 2h-12a2 2 0 0 1 -2 -2v-16a2 2 0 0 1 2 -2Z"),
    TravelMap("M14.106 5.553a2 2 0 0 0 1.788 0l3.659-1.83A1 1 0 0 1 21 4.619v12.764a1 1 0 0 1-.553.894l-4.553 2.277a2 2 0 0 1-1.788 0l-4.212-2.106a2 2 0 0 0-1.788 0l-3.659 1.83A1 1 0 0 1 3 19.381V6.618a1 1 0 0 1 .553-.894l4.553-2.277a2 2 0 0 1 1.788 0zM15 5.764v15M9 3.236v15"),
    Tent("M3.5 21 14 3M20.5 21 10 3M15.5 21 12 15l-3.5 6M2 21h20"),
    Mountain("M8 3l4 8 5-5 5 15H2L8 3z"),
    Train("M8 3.1V7a4 4 0 0 0 8 0V3.1M0 0m9 15-1-1M0 0m15 15 1-1M9 19c-2.8 0-5-2.2-5-5v-4a8 8 0 0 1 16 0v4c0 2.8-2.2 5-5 5ZM0 0m8 19-2 3M0 0m16 19 2 3"),
    Taxi("M10 2h4M0 0m21 8-2 2-1.5-3.7A2 2 0 0 0 15.646 5H8.4a2 2 0 0 0-1.903 1.257L5 10 3 8M7 14h.01M17 14h.01M5 10h14a2 2 0 0 1 2 2v4a2 2 0 0 1 -2 2h-14a2 2 0 0 1 -2 -2v-4a2 2 0 0 1 2 -2ZM5 18v2M19 18v2"),
    Ship("M12 10.189V14M12 2v3M19 13V7a2 2 0 0 0-2-2H7a2 2 0 0 0-2 2v6M19.38 20A11.6 11.6 0 0 0 21 14l-8.188-3.639a2 2 0 0 0-1.624 0L3 14a11.6 11.6 0 0 0 2.81 7.76M2 21c.6.5 1.2 1 2.5 1 2.5 0 2.5-2 5-2 1.3 0 1.9.5 2.5 1s1.2 1 2.5 1c2.5 0 2.5-2 5-2 1.3 0 1.9.5 2.5 1"),
    Ticket("M2 9a3 3 0 0 1 0 6v2a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-2a3 3 0 0 1 0-6V7a2 2 0 0 0-2-2H4a2 2 0 0 0-2 2ZM13 5v2M13 17v2M13 11v2"),
    Camera("M14.5 4h-5L7 7H4a2 2 0 0 0-2 2v9a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2V9a2 2 0 0 0-2-2h-3l-2.5-3zM9 13a3 3 0 1 0 6 0a3 3 0 1 0 -6 0"),
    Pizza("M15 11h.01M11 15h.01M16 16h.01M2 16l20 6-6-20A20 20 0 0 0 2 16M5.71 17.11a17.04 17.04 0 0 1 11.4-11.4"),
    Rice("M12 21a9 9 0 0 0 9-9H3a9 9 0 0 0 9 9ZM7 21h10M5.5 12C6 8 8.5 6 12 6s6 2 6.5 6"),
    Soda("M6 8l1.75 12.28a2 2 0 0 0 2 1.72h4.54a2 2 0 0 0 2-1.72L18 8M5 8h14M7 15a6.47 6.47 0 0 1 5 0 6.47 6.47 0 0 0 5 0M12 8l1-6h2"),
    Beer("M17 11h1a3 3 0 0 1 0 6h-1M9 12v6M13 12v6M14 7.5c-1 0-1.44.5-3 .5s-2-.5-3-.5-1.72.5-2.5.5a2.5 2.5 0 0 1 0-5c.78 0 1.57.5 2.5.5S9.44 2 11 2s2 1.5 3 1.5 1.72-.5 2.5-.5a2.5 2.5 0 0 1 0 5c-.78 0-1.5-.5-2.5-.5M5 8v12a2 2 0 0 0 2 2h8a2 2 0 0 0 2-2V8"),
    Wine("M8 22h8M7 10h10M12 15v7M12 15a5 5 0 0 0 5-5c0-2-.5-4-2-8H9c-1.5 4-2 6-2 8a5 5 0 0 0 5 5Z"),
    IceCream("M7 11l4.08 10.35a1 1 0 0 0 1.84 0L17 11M17 7A5 5 0 0 0 7 7M17 7a2 2 0 0 1 0 4H7a2 2 0 0 1 0-4"),
    Watermelon("M21.66 17.67a1.08 1.08 0 0 1-.04 1.6A12 12 0 0 1 4.73 2.38a1.1 1.1 0 0 1 1.61-.04zM19.65 15.66A8 8 0 0 1 8.35 4.34M14 10l-5.5 5.5M14 17.85V10H6.15"),
    Baby("M10 16c.5.3 1.2.5 2 .5s1.5-.2 2-.5M15 12h.01M19.38 6.813A9 9 0 0 1 20.8 10.2a2 2 0 0 1 0 3.6 9 9 0 0 1-17.6 0 2 2 0 0 1 0-3.6A9 9 0 0 1 12 3c2 0 3.5 1.1 3.5 2.5s-.9 2.5-2 2.5c-.8 0-1.5-.4-1.5-1M9 12h.01"),
    Paw("M11 4a2 2 0 1 0 4 0a2 2 0 1 0 -4 0M18 8a2 2 0 1 0 4 0a2 2 0 1 0 -4 0M2 12a2 2 0 1 0 4 0a2 2 0 1 0 -4 0M9 10a5 5 0 0 1 5 5v3.5a3.5 3.5 0 0 1-6.84 1.045Q6.52 17.48 4.46 16.84A3.5 3.5 0 0 1 5.5 10Z"),
    Shield("M20 13c0 5-3.5 7.5-7.66 8.95a1 1 0 0 1-.67-.01C7.5 20.5 4 18 4 13V6a1 1 0 0 1 1-1c2 0 4.5-1.2 6.24-2.72a1.17 1.17 0 0 1 1.52 0C14.51 3.81 17 5 19 5a1 1 0 0 1 1 1z"),
    Tooth("M7 3c-2 0-4 1.5-4 4.5 0 2 1 3.5 1.5 5.5.6 2.5 1 8 3 8 1.5 0 1.5-4.5 2.5-6 .5-.8 1.5-.8 2 0 1 1.5 1 6 2.5 6 2 0 2.4-5.5 3-8 .5-2 1.5-3.5 1.5-5.5C21 4.5 19 3 17 3c-2 0-3 1-5 1S9 3 7 3Z"),
    Syringe("M18 2l4 4M17 7l3-3M19 9 8.7 19.3c-1 1-2.5 1-3.4 0l-.6-.6c-1-1-1-2.5 0-3.4L15 5M9 11l4 4M5 19l-3 3M14 4l6 6"),
    Glasses("M2 15a4 4 0 1 0 8 0a4 4 0 1 0 -8 0M14 15a4 4 0 1 0 8 0a4 4 0 1 0 -8 0M14 15a2 2 0 0 0-2-2 2 2 0 0 0-2 2M2.5 13 5 7c.7-1.3 1.4-2 3-2M21.5 13 19 7c-.7-1.3-1.5-2-3-2"),
    School("M14 22v-4a2 2 0 1 0-4 0v4M18 10l3.447 1.724a1 1 0 0 1 .553.894V20a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2v-7.382a1 1 0 0 1 .553-.894L6 10M18 5v17M4 6l7.106-3.553a2 2 0 0 1 1.788 0L20 6M6 5v17M10 9a2 2 0 1 0 4 0a2 2 0 1 0 -4 0"),
    Laundry("M3 6h3M17 6h.01M5 2h14a2 2 0 0 1 2 2v16a2 2 0 0 1 -2 2h-14a2 2 0 0 1 -2 -2v-16a2 2 0 0 1 2 -2ZM7 13a5 5 0 1 0 10 0a5 5 0 1 0 -10 0M12 18a2.5 2.5 0 0 0 0-5 2.5 2.5 0 0 1 0-5"),
    Sofa("M20 9V6a2 2 0 0 0-2-2H6a2 2 0 0 0-2 2v3M2 16a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-5a2 2 0 0 0-4 0v1.5a.5.5 0 0 1-.5.5h-11a.5.5 0 0 1-.5-.5V11a2 2 0 0 0-4 0zM4 18v2M20 18v2M12 4v9"),
    Truck("M14 18V6a2 2 0 0 0-2-2H4a2 2 0 0 0-2 2v11a1 1 0 0 0 1 1h2M15 18H9M19 18h2a1 1 0 0 0 1-1v-3.65a1 1 0 0 0-.22-.624l-3.48-4.35A1 1 0 0 0 17.52 8H14M5 18a2 2 0 1 0 4 0a2 2 0 1 0 -4 0M15 18a2 2 0 1 0 4 0a2 2 0 1 0 -4 0"),
    Newspaper("M15 18h-5M18 14h-8M4 22h16a2 2 0 0 0 2-2V4a2 2 0 0 0-2-2H8a2 2 0 0 0-2 2v16a2 2 0 0 1-4 0v-9a2 2 0 0 1 2-2h2M10 6h8v4h-8V6Z"),
    Barber("M8 2h8M8 22h8M9 2v20M15 2v20M9 6l6 4M9 12l6 4"),
    Palette("M12 22a1 1 0 0 1 0-20 10 9 0 0 1 10 9 5 5 0 0 1-5 5h-2.25a1.75 1.75 0 0 0-1.4 2.8l.3.4a1.75 1.75 0 0 1-1.4 2.8zM13 6.5a0.5 0.5 0 1 0 1 0a0.5 0.5 0 1 0 -1 0M17 10.5a0.5 0.5 0 1 0 1 0a0.5 0.5 0 1 0 -1 0M6 12.5a0.5 0.5 0 1 0 1 0a0.5 0.5 0 1 0 -1 0M8 7.5a0.5 0.5 0 1 0 1 0a0.5 0.5 0 1 0 -1 0"),
    Ring("M12 8a6 6 0 1 0 0 12a6 6 0 1 0 0 -12M9 2h6l1.5 3L12 8 7.5 5Z"),
    Flower("M12 16a4 4 0 1 0 0-8 4 4 0 0 0 0 8ZM12 8V2M12 22v-6M8 12H2M22 12h-6M9 9 5 5M19 19l-4-4M9 15l-4 4M19 5l-4 4"),
    Party("M5.8 11.3 2 22l10.7-3.79M4 3h.01M22 8h.01M15 2h.01M22 20h.01M11 13c1.93 1.93 2.83 4.17 2 5-.83.83-3.07-.07-5-2-1.93-1.93-2.83-4.17-2-5 .83-.83 3.07.07 5 2Z"),
    Charity("M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7ZM12 5 9.04 7.96a2.17 2.17 0 0 0 0 3.08c.82.82 2.13.85 3 .07l2.07-1.9a2.82 2.82 0 0 1 3.79 0l2.96 2.66"),
    Handshake("M11 17l2 2a1 1 0 1 0 3-3M14 14l2.5 2.5a1 1 0 1 0 3-3l-3.88-3.88a3 3 0 0 0-4.24 0l-.88.88a1 1 0 1 1-3-3l2.81-2.81a5.79 5.79 0 0 1 7.06-.87l.47.28a2 2 0 0 0 1.42.25L21 4M21 3l1 11h-2M3 3 2 14l6.5 6.5a1 1 0 1 0 3-3M3 4h8"),
    Calculator("M6 2h12a2 2 0 0 1 2 2v16a2 2 0 0 1 -2 2h-12a2 2 0 0 1 -2 -2v-16a2 2 0 0 1 2 -2ZM8 6h8M16 14v4M16 10h.01M12 10h.01M8 10h.01M12 14h.01M8 14h.01M12 18h.01M8 18h.01"),
    Backspace("M10 5a2 2 0 0 0-1.344.519l-6.328 5.74a1 1 0 0 0 0 1.481l6.328 5.741A2 2 0 0 0 10 19h10a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2zM0 0m12 9 6 6M0 0m18 9-6 6");

    companion object {
        private val byName = entries.associateBy { it.name }
        /** Registry lookup for icons stored in Room as iconKey; unknown keys fall back to More. */
        fun fromKey(key: String?): MIcon = byName[key] ?: More

        /**
         * Colour emoji for category/vault icons, drawn by the system emoji font: no assets, no APK growth.
         * Kept to Unicode <= 10 where possible so Android 8+ renders them. UI chrome icons (nav, arrows) stay as strokes.
         */
        private val emojis: Map<MIcon, String> = mapOf(
            Utensils to "🍽️", Burger to "🍔", Coffee to "☕", Car to "🚗", Plane to "✈️", ShoppingBag to "🛍️", Receipt to "🧾", Phone to "📱",
            House to "🏠", Home to "🏡", Heart to "❤️", Graduation to "🎓", Gift to "🎁", Briefcase to "💼", Wallet to "👛",
            Sparkles to "✨", Users to "👥", Chart to "📊", Calendar to "📅", Transfer to "🔁", Sun to "☀️", Moon to "🌙",
            Settings to "🛠️", Search to "🔍", CreditCard to "💳", Landmark to "🏦", Droplet to "💧", Lightbulb to "💡",
            Music to "🎵", Film to "🎬", Gamepad to "🎮", Shirt to "👕", Book to "📚", Tv to "📺", Wifi to "📶", Pill to "💊",
            Fuel to "⛽", Bike to "🚲", Bus to "🚌", Coins to "💴", TrendingUp to "📈", Cake to "🎂", Scissors to "✂️",
            Umbrella to "☂️", Laptop to "💻", Cart to "📦", Tag to "🏷️", Star to "⭐",
            Noodles to "🍜", Apple to "🍎", Motorcycle to "🏍️", SprayBottle to "🧴", Dumbbell to "🏋️", Hospital to "🏥",
            Ball to "⚽", Lipstick to "💄", FileCheck to "📜", MoneyOut to "💸", PiggyBank to "🐷", Building to "🏢", Key to "🔑",
            Popcorn to "🍿", Cookie to "🍪", Landline to "☎️", Basket to "🛒", Soap to "🧼", Dress to "👗", Jeans to "👖",
            HourlyPay to "⏰", Trophy to "🏆", Candles to "📊", MoneyBag to "💰", Banknote to "💵", Wrench to "🔧",
            Parking to "🅿️", Gem to "💎", Bed to "🛏️", CookingPot to "🍲", Waves to "🏊", Billiard to "🎱", Shoe to "👟",
            Zap to "⚡", Faucet to "🚰", Flame to "🔥", Globe to "🌐", Cloud to "☁️", Subscription to "🔄", Plug to "🔌", Luggage to "🧳", PalmTree to "🏝️", Hotel to "🏨", TravelMap to "🗺️", Tent to "⛺", Mountain to "⛰️", Train to "🚆", Taxi to "🚕", Ship to "⛴️", Ticket to "🎟️", Camera to "📷", Pizza to "🍕", Rice to "🍚", Soda to "🥤", Beer to "🍺", Wine to "🍷", IceCream to "🍦", Watermelon to "🍉", Baby to "🍼", Paw to "🐾", Shield to "🛡️", Tooth to "🦷", Syringe to "💉", Glasses to "👓", School to "🏫", Laundry to "🧺", Sofa to "🛋️", Truck to "🚚", Newspaper to "📰", Barber to "💈", Palette to "🎨", Ring to "💍", Flower to "💐", Party to "🎉", Charity to "💝", Handshake to "🤝", Calculator to "🧮",
        )

        /** Icons offered when creating or editing a category, grouped by theme (food, travel, bills, ...). */
        val picker: List<MIcon> = listOf(
            // Food & drink
            Utensils, Burger, Noodles, Rice, Pizza, CookingPot, Coffee, Soda, Beer, Wine, Cake, IceCream, Cookie, Popcorn, Apple, Watermelon, Basket,
            // Transport & travel
            Car, Taxi, Motorcycle, Bike, Bus, Train, Plane, Ship, Fuel, Parking, Wrench, Luggage, PalmTree, Hotel, TravelMap, Tent, Mountain, Ticket, Camera,
            // Bills & utilities
            Zap, Faucet, Droplet, Flame, Wifi, Globe, Phone, Landline, Tv, Cloud, Subscription, Plug, Receipt, FileCheck, Lightbulb,
            // Shopping & beauty
            ShoppingBag, Cart, Shirt, Dress, Jeans, Shoe, Gem, Lipstick, Soap, SprayBottle, Tag, Laptop,
            // Home
            House, Home, Building, Key, Bed, Sofa, Laundry, Truck, Newspaper, Umbrella,
            // Health, family & education
            Heart, Hospital, Pill, Syringe, Tooth, Glasses, Dumbbell, Baby, Paw, Users, Graduation, School, Book, Shield,
            // Fun & gifts
            Music, Film, Gamepad, Ball, Billiard, Waves, Palette, Party, Flower, Ring, Gift, Barber, Scissors, Star, Trophy, Sparkles,
            // Money & work
            Wallet, Briefcase, CreditCard, Landmark, Coins, Banknote, MoneyBag, MoneyOut, PiggyBank, TrendingUp, Candles, HourlyPay, Calculator, Charity, Handshake, Transfer,
            // Other
            Chart, Calendar, Clock, Sun, Moon, Settings, Search, More,
        )
    }

    val emoji: String? get() = emojis[this]

    val vector: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).addPath(
            pathData = addPathNodes(path),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ).build()
    }
}

/** Category/vault icon: colour emoji when the icon has one, otherwise the tinted stroke vector. */
@Composable
fun EmojiIcon(icon: MIcon, tint: Color, size: Dp = 20.dp) {
    // Newer emoji (e.g. 🧳 🦷 🧺) are missing from old system fonts: fall back to the stroke icon instead of a tofu box.
    val e = icon.emoji?.takeIf { EmojiSupport.has(it) }
    if (e == null) MIconView(icon, tint, size = size)
    else Text(e, fontSize = (size.value * 0.9f).sp, lineHeight = (size.value * 1.1f).sp, textAlign = TextAlign.Center, modifier = Modifier.size(size))
}

@Composable
fun MIconView(icon: MIcon, tint: Color, modifier: Modifier = Modifier, size: Dp = 20.dp) {
    Icon(imageVector = icon.vector, contentDescription = null, tint = tint, modifier = modifier.size(size))
}

/** Caches Paint.hasGlyph per emoji: one native lookup per icon for the app's lifetime. */
private object EmojiSupport {
    private val paint by lazy(LazyThreadSafetyMode.NONE) { android.graphics.Paint() }
    private val cache = HashMap<String, Boolean>()
    fun has(emoji: String): Boolean = cache.getOrPut(emoji) { runCatching { paint.hasGlyph(emoji) }.getOrDefault(true) }
}
