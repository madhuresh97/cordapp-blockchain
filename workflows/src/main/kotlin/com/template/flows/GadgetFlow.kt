package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.GadgetContract
import com.template.states.GadgetState
import net.corda.core.contracts.Command
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.ServiceHub
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
@StartableByRPC
class CarIssueInitiator(
        val owningBank: Party,
        val holdingDealer: Party,
        val manufacturer: Party,
        val vin: String,
        val licensePlateNumber: String,
        val make: String,
        val model: String,
        val dealershipLocation: String
) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {

        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val command = Command(CarContract.Commands.Issue(), listOf(owningBank, holdingDealer, manufacturer).map { it.owningKey })
        val carState = CarState(
                owningBank,
                holdingDealer,
                manufacturer,
                vin,
                licensePlateNumber,
                make,
                model,
                dealershipLocation,
                UniqueIdentifier()
        )

        val txBuilder = TransactionBuilder(notary)
                .addOutputState(carState, CarContract.ID)
                .addCommand(command)

        txBuilder.verify(serviceHub)
        val tx = serviceHub.signInitialTransaction(txBuilder)

        val sessions = (carState.participants - ourIdentity).map { initiateFlow(it as Party) }
        val stx = subFlow(CollectSignaturesFlow(tx, sessions))
        return subFlow(FinalityFlow(stx, sessions))
    }
}

@InitiatedBy(CarIssueInitiator::class)
class CarIssueResponder(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val signedTransactionFlow = object : SignTransactionFlow(counterpartySession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val output = stx.tx.outputs.single().data
                "The output must be a CarState" using (output is CarState)
            }
        }
        val txWeJustSignedId = subFlow(signedTransactionFlow)
        return subFlow(ReceiveFinalityFlow(counterpartySession, txWeJustSignedId.id))
    }
}