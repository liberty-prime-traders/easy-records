package app.myliberty.easy_records.organizations.business.core.account_selection.api

import app.myliberty.easy_records.util.ui_models.TreeNode

data class AccountsTreesForSelection(
    val payable: List<TreeNode<String>>,
    val recoverable: List<TreeNode<String>>,
    val paymentMethods: List<TreeNode<String>>,
    val expenseTypes: List<TreeNode<String>>,
    val fundMovements: List<TreeNode<String>>,
    val loans: List<TreeNode<String>>,
    val manualJournals: List<TreeNode<String>>,
)
