package com.template.states

import com.template.contracts.GadgetContract
import com.template.contracts.TemplateContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import java.util.*

// *********
// * State *
// *********
@BelongsToContract(GadgetContract::class)
data class GadgetState(
        val to: AbstractParty,
        val from: AbstractParty,
        val productID: UUID,
        val productName: String,
        val productColour: String,
        val Status: String
): ContractState{
    override val participants: List<AbstractParty> = listOf(to, from)
}