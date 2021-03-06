/*
 *  Copyright 2016 DTCC, Fujitsu Australia Software Technology, IBM - All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 	  http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.hyperledger.fabric.sdk.transaction;

import java.util.Arrays;
import java.util.List;

import javax.xml.bind.DatatypeConverter;

import com.google.protobuf.ByteString;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperledger.fabric.protos.common.Common;
import org.hyperledger.fabric.protos.common.Common.HeaderType;
import org.hyperledger.fabric.protos.msp.Identities;
import org.hyperledger.fabric.protos.peer.Chaincode;
import org.hyperledger.fabric.protos.peer.Chaincode.ChaincodeInput;
import org.hyperledger.fabric.protos.peer.Chaincode.ChaincodeInvocationSpec;
import org.hyperledger.fabric.protos.peer.Chaincode.ChaincodeSpec;
import org.hyperledger.fabric.protos.peer.FabricProposal;
import org.hyperledger.fabric.protos.peer.FabricProposal.ChaincodeHeaderExtension;

import static org.hyperledger.fabric.sdk.transaction.ProtoUtils.createChannelHeader;


public class ProposalBuilder {


    private final Log logger = LogFactory.getLog(ProposalBuilder.class);


    private Chaincode.ChaincodeID chaincodeID;
    private List<ByteString> argList;
    protected TransactionContext context;
    private ChaincodeSpec.Type ccType = ChaincodeSpec.Type.GOLANG;


    protected ProposalBuilder() {
    }

    public static ProposalBuilder newBuilder() {
        return new ProposalBuilder();
    }

    public ProposalBuilder chaincodeID(Chaincode.ChaincodeID chaincodeID) {
        this.chaincodeID = chaincodeID;
        return this;
    }

    public ProposalBuilder args(List<ByteString> argList) {
        this.argList = argList;
        return this;
    }

    public ProposalBuilder context(TransactionContext context) {
        this.context = context;
        return this;
    }


    public FabricProposal.Proposal build() throws Exception {
        return createFabricProposal(context.getChain().getName(), chaincodeID, argList);
    }


    private FabricProposal.Proposal createFabricProposal(String chainID, Chaincode.ChaincodeID chaincodeID, List<ByteString> argList)  {

        ChaincodeInvocationSpec chaincodeInvocationSpec = createChaincodeInvocationSpec(
                chaincodeID,
                ccType, argList);


        ChaincodeHeaderExtension.Builder chaincodeHeaderExtension = ChaincodeHeaderExtension.newBuilder();


        chaincodeHeaderExtension.setChaincodeId(chaincodeID);


        Common.ChannelHeader chainHeader = createChannelHeader(HeaderType.ENDORSER_TRANSACTION,
                context.getTxID(), chainID, 0, chaincodeHeaderExtension.build());

        Common.SignatureHeader.Builder sigHeaderBldr = Common.SignatureHeader.newBuilder();

        Identities.SerializedIdentity.Builder identity = Identities.SerializedIdentity.newBuilder();
        identity.setIdBytes(ByteString.copyFromUtf8(context.getCreator()));
        identity.setMspid(context.getMSPID());


        sigHeaderBldr.setCreator(identity.build().toByteString());
        sigHeaderBldr.setNonce(context.getNonce());

        Common.SignatureHeader sigHeader = sigHeaderBldr.build();
        logger.trace("proposal header sig bytes:" + Arrays.toString(sigHeader.toByteArray()));


        Common.Header.Builder headerbldr = Common.Header.newBuilder();
        headerbldr.setSignatureHeader(sigHeader.toByteString());
        headerbldr.setChannelHeader(chainHeader.toByteString());

        FabricProposal.ChaincodeProposalPayload.Builder payloadBuilder = FabricProposal.ChaincodeProposalPayload.newBuilder();

        payloadBuilder.setInput(chaincodeInvocationSpec.toByteString());
        FabricProposal.ChaincodeProposalPayload payload = payloadBuilder.build();

      //  logger.trace("proposal payload. length " + payload.toByteArray().length + ",  hashcode:" + payload.toByteArray().hashCode() + ", hex:" + DatatypeConverter.printHexBinary(payload.toByteArray()));
      //  logger.trace("256 HASH: " + DatatypeConverter.printHexBinary(context.getCryptoPrimitives().hash(payload.toByteArray())));


        FabricProposal.Proposal.Builder proposalBuilder = FabricProposal.Proposal.newBuilder();


        Common.Header header = headerbldr.build();
        //  logger.trace("proposal header bytes:" + Arrays.toString(header.toByteArray()));

        proposalBuilder.setHeader(header.toByteString());
        proposalBuilder.setPayload(payload.toByteString());


        return proposalBuilder.build();

    }


    private ChaincodeInvocationSpec createChaincodeInvocationSpec(Chaincode.ChaincodeID chainCodeId, ChaincodeSpec.Type langType, List<ByteString> args) {

        ChaincodeInput chaincodeInput = ChaincodeInput.newBuilder().addAllArgs(args).build();

        ChaincodeSpec chaincodeSpecBuilder = ChaincodeSpec.newBuilder()
                .setType(langType)
                .setChaincodeId(chainCodeId)
                .setInput(chaincodeInput)
                .build();

        ChaincodeInvocationSpec.Builder invocationSpecBuilder = ChaincodeInvocationSpec.newBuilder()
                .setChaincodeSpec(chaincodeSpecBuilder);

        return invocationSpecBuilder.build();
    }


    public ProposalBuilder ccType(ChaincodeSpec.Type ccType) {
        this.ccType = ccType;
        return this;
    }
}