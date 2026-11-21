package app.myliberty.easy_records.util.enums

enum class EsrRole(
    override val code: String,
    val tier: SchemaLevel,
    val permissions: Set<EsrPermission> = emptySet(),
    val requiredRoles: List<EsrRole> = emptyList()
) : HasCode {

    MANAGE_ORGANIZATION_USERS(
        "MOGU",
        SchemaLevel.ORGANIZATION,
        permissions = setOf(
            EsrPermission.ADD_USER_TO_LOCATION,
            EsrPermission.REMOVE_USER_FROM_LOCATION,
            EsrPermission.REMOVE_USER_FROM_ORG,
            EsrPermission.VIEW_USERS_OF_LOCATION,
            EsrPermission.VIEW_USERS_OF_ORG
        )
    ),

    VIEW_ORGANIZATION_USERS(
        "VOGU",
        SchemaLevel.ORGANIZATION,
        permissions = setOf(
            EsrPermission.VIEW_USERS_OF_LOCATION,
            EsrPermission.VIEW_USERS_OF_ORG
        )
    ),

    MANAGE_MANUAL_JOURNALS(
        "MMJR",
        SchemaLevel.ORGANIZATION,
        permissions = setOf(
            EsrPermission.CREATE_MANUAL_JOURNAL,
            EsrPermission.REVERSE_MANUAL_JOURNAL,
            EsrPermission.VIEW_MANUAL_JOURNAL
        )
    ),

    LIBERTY_ADMIN(
        "LBAR",
        SchemaLevel.ORGANIZATION,
        permissions = setOf(EsrPermission.LIBERTY_ADMIN)
    ),

    PLATFORM_ADMIN("PLAD", SchemaLevel.PLATFORM, permissions = setOf(EsrPermission.CREATE_ORGANIZATION))
}
