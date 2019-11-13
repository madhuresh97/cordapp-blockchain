package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.GadgetContract
import com.template.states.GadgetState
import net.corda.core.contracts.Command
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.node.ServiceHub
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import java.util.*

@InitiatingFlow
@StartableByRPC
// createProductFlow
class CompanyAInitiator(
        val to: AbstractParty,
        val from: AbstractParty,
        val productID: UUID,
        val productName: String,
        val productColour: String,
        val Status: String
) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {

        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val command = Command(GadgetContract.Commands.Issue(), listOf(to, from).map { it.owningKey })
        val gadgetState = GadgetState(
                to,
                from,
                productID,
                productName,
                productColour,
                Status//,
              //  UniqueIdentifier()
        )

        val txBuilder = TransactionBuilder(notary)
                .addOutputState(gadgetState, GadgetContract.ID)
                .addCommand(command)

        txBuilder.verify(serviceHub)
        val tx = serviceHub.signInitialTransaction(txBuilder)

        val sessions = (gadgetState.participants - ourIdentity).map { initiateFlow(it as Party) }   //Takes both the companies in list
        val stx = subFlow(CollectSignaturesFlow(tx, sessions))          // collects signatures of both companies A and B
        return subFlow(FinalityFlow(stx, sessions))     // Finalizes the transaction to the notary
    }
}

@InitiatedBy(CompanyAInitiator::class)
class CompanyAResponder(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val signedTransactionFlow = object : SignTransactionFlow(counterpartySession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val output = stx.tx.outputs.single().data
                "The output must be a GadgetState" using (output is GadgetState)
            }
        }
        val txWeJustSignedId = subFlow(signedTransactionFlow)
        return subFlow(ReceiveFinalityFlow(counterpartySession, txWeJustSignedId.id))
    }
}

// For Company B to A
// UpdateProductFlow
@InitiatingFlow
@StartableByRPC
class CompanyBInitiator(
        val to: AbstractParty,
        val from: AbstractParty,
        val productID: UUID,
        val productName: String,
        val productColour: String,
        val Status: String
) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {

        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val command = Command(GadgetContract.Commands.Issue(), listOf(to, from).map { it.owningKey })
        val gadgetState = GadgetState(
                to,
                from,
                productID,
                productName,
                productColour,
                Status//,
                //  UniqueIdentifier()
        )

        val txBuilder = TransactionBuilder(notary)
                .addOutputState(gadgetState, GadgetContract.ID)
                .addCommand(command)

        txBuilder.verify(serviceHub)
        val tx = serviceHub.signInitialTransaction(txBuilder)

        val sessions = (gadgetState.participants - ourIdentity).map { initiateFlow(it as Party) }   //Takes both the companies in list
        val stx = subFlow(CollectSignaturesFlow(tx, sessions))          // collects signatures of both companies A and B
        return subFlow(FinalityFlow(stx, sessions))     // Finalizes the transaction to the notary
    }
}

@InitiatedBy(CompanyBInitiator::class)
class CompanyBResponder(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val signedTransactionFlow = object : SignTransactionFlow(counterpartySession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val output = stx.tx.outputs.single().data
                "The Product has been received" using (output is GadgetState)
            }
        }
        val txWeJustSignedId = subFlow(signedTransactionFlow)
        return subFlow(ReceiveFinalityFlow(counterpartySession, txWeJustSignedId.id))
    }
}
