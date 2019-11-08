package com.template.contracts

import com.template.states.GadgetState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction

// ************
// * Contract *
// ************
class GadgetContract : Contract {
    companion object {
        const val ID = "com.template.contracts.GadgetContract"
    }

    override fun verify(tx: LedgerTransaction) {

        when(tx.commands.requireSingleCommand<Commands>().value) {
            is Commands.Issue -> requireThat {
                val outputState = tx.outputs[0].data as GadgetState
                "The Product colour should be only Red or Black" using (outputState.productColour == "Red")
            }
        }
    }

    interface Commands : CommandData {
        class Issue : Commands
    }
}