package app.myliberty.easy_records.organizations.business.core.account_selection

import app.myliberty.easy_records.organizations.business.core.account.api.AccountSelectionCandidate
import app.myliberty.easy_records.organizations.business.core.account_selection.api.AccountSelectionRule
import app.myliberty.easy_records.util.ui_models.TreeNode

object AccountTreeBuilder {

    fun build(candidates: List<AccountSelectionCandidate>, rule: AccountSelectionRule): List<TreeNode<String>> {
        val activeChildrenByParentCode = candidates.filter { it.accountIsActive }.groupBy { it.parentAccountCode }
        return activeChildrenByParentCode[null].orEmpty()
            .mapNotNull { buildNode(it, activeChildrenByParentCode, rule) }
    }

    private fun buildNode(
        candidate: AccountSelectionCandidate,
        activeChildrenByParentCode: Map<String?, List<AccountSelectionCandidate>>,
        rule: AccountSelectionRule
    ): TreeNode<String>? {
        val children = activeChildrenByParentCode[candidate.code].orEmpty()
            .mapNotNull { buildNode(it, activeChildrenByParentCode, rule) }

        val selectable = rule.isSelectable(candidate)
        if (!selectable && children.isEmpty()) return null

        return TreeNode(
            key = candidate.code,
            label = candidate.label,
            selectable = selectable,
            children = children
        )
    }
}
