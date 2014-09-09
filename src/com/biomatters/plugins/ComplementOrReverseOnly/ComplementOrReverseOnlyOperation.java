package com.biomatters.plugins.ComplementOrReverseOnly;

import com.biomatters.geneious.publicapi.documents.AnnotatedPluginDocument;
import com.biomatters.geneious.publicapi.documents.sequence.DefaultSequenceListDocument;
import com.biomatters.geneious.publicapi.documents.sequence.NucleotideSequenceDocument;
import com.biomatters.geneious.publicapi.documents.sequence.SequenceCharSequence;
import com.biomatters.geneious.publicapi.documents.sequence.SequenceDocument;
import com.biomatters.geneious.publicapi.implementations.SequenceExtractionUtilities;
import com.biomatters.geneious.publicapi.implementations.sequence.DefaultSequenceDocument;
import com.biomatters.geneious.publicapi.plugin.*;
import com.biomatters.geneious.publicapi.utilities.Interval;
import com.biomatters.geneious.publicapi.utilities.SequenceUtilities;
import com.biomatters.geneious.publicapi.utilities.StandardIcons;
import jebl.evolution.sequences.NucleotideState;
import jebl.evolution.sequences.Nucleotides;
import jebl.util.ProgressListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Copyright (C) 2013-2014, Biomatters Ltd
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
public class ComplementOrReverseOnlyOperation extends DocumentOperation {

    private boolean isComplementOnlyRatherThanReverseOnly;
    private String name;
    private String description;

    public ComplementOrReverseOnlyOperation(String name, String description, boolean isComplementOnlyRatherThanReverseOnly) {
        this.name = name;
        this.description = description;
        this.isComplementOnlyRatherThanReverseOnly = isComplementOnlyRatherThanReverseOnly;

    }

    @Override
    public GeneiousActionOptions getActionOptions() {
        return new GeneiousActionOptions(name+"...",description).setMainMenuLocation(GeneiousActionOptions.MainMenu.Sequence, 0.15);
    }

    @Override
    public String getHelp() {
        return description;
    }

    @Override
    public String getUniqueId() {
        return name;
    }

    @Override
    public DocumentSelectionSignature[] getSelectionSignatures() {
        return new DocumentSelectionSignature[]{DocumentSelectionSignature.forNucleotideSequences(1,Integer.MAX_VALUE,true)};
    }

    @Override
    public Options getOptions(SequenceSelection sequenceSelection, AnnotatedPluginDocument... documents) throws DocumentOperationException {
        return new ComplementOrReverseOnlyOptions(name, sequenceSelection);
    }

    public Options getGeneralOptions() throws DocumentOperationException {
        return new ComplementOrReverseOnlyOptions(name, null);
    }

    private static boolean isAnythingSelected(SequenceSelection sequenceSelection) {
        return sequenceSelection!=null && !sequenceSelection.getNonZeroLengthIntervals().isEmpty();
    }

    private static class ComplementOrReverseOnlyOptions extends Options {
        OptionValue all;
        OptionValue selection;
        private RadioOption<OptionValue> whatToExtract;

        private ComplementOrReverseOnlyOptions(String name, SequenceSelection sequenceSelection) {
            name = name.toLowerCase();
            name = name.substring(0,1).toUpperCase()+name.substring(1);
            all = new OptionValue("all",name+" and extract entire sequence(s)");
            selection = new OptionValue("selection",name+" and extract the selected region(s)");

            if (!isAnythingSelected(sequenceSelection)) {
                selection.setEnabled(false);
            }

            beginAlignHorizontally(null,false);
            whatToExtract = addRadioOption("whatToExtract", "", new OptionValue[]{selection, all}, selection, Alignment.VERTICAL_ALIGN);
            endAlignHorizontally();
            beginAlignHorizontally(null,false);
            addLabel(" ");
            endAlignHorizontally();
            beginAlignHorizontally(null,false);
            addLabelWithIcon("<html><b><center>'" + name + "' is a very specialized function. In most<br>cases you should use Reverse-Complement instead.</center></b></html>", StandardIcons.warning.getIcons());
            endAlignHorizontally();
        }

        public boolean extractSelection() {
            return whatToExtract.getValue().equals(selection);
        }
    }

    @Override
    public void performOperation(AnnotatedPluginDocument[] annotatedDocuments, ProgressListener progressListener, Options _options, SequenceSelection sequenceSelection, OperationCallback callback) throws DocumentOperationException {
        List<AnnotatedPluginDocument> documents = Arrays.asList(annotatedDocuments);
        ComplementOrReverseOnlyOptions options = (ComplementOrReverseOnlyOptions) _options;
        List<NucleotideSequenceDocument> results = new ArrayList<NucleotideSequenceDocument>();
        if (options.extractSelection()) {
            List<SequenceSelection.SelectionInterval> intervals = sequenceSelection.getIntervals(true);
            for (SequenceSelection.SelectionInterval interval : intervals) {
                SequenceDocument sequence = interval.getSequenceIndex().getSequence(documents);
                Interval fullInterval = interval.getResidueInterval();
                SequenceDocument result = SequenceExtractionUtilities.extract(sequence, new SequenceExtractionUtilities.ExtractionOptions(fullInterval).setRemoveGaps(true));
                results.add(applyOperationToSequence(result));
            }
        }
        else {
            for (SequenceDocument sequence : SequenceUtilities.getSequences(annotatedDocuments, SequenceDocument.Alphabet.NUCLEOTIDE, ProgressListener.EMPTY)) {
                sequence = SequenceExtractionUtilities.removeGaps(sequence);
                results.add(applyOperationToSequence(sequence));
            }
        }
        if (results.size()>1) {
            DefaultSequenceListDocument sequenceList = DefaultSequenceListDocument.forNucleotideSequences(results);
            sequenceList.setName(results.size()+" "+name+" sequences");
            callback.addDocument(sequenceList,false,ProgressListener.EMPTY);
        }
        else if (results.size()>0) {
            callback.addDocument(results.get(0),false,ProgressListener.EMPTY);
        }
    }        

    private NucleotideSequenceDocument applyOperationToSequence(SequenceDocument sequence) {
        if (!isComplementOnlyRatherThanReverseOnly) { // for reverse only, we'll reverse complement then complement again
            sequence = SequenceExtractionUtilities.reverseComplement(sequence);
        }
        String name =sequence.getName();
        DefaultSequenceDocument newSequence = SequenceUtilities.createSequenceCopyEditable(sequence);
        SequenceCharSequence charSequence = newSequence.getCharSequence();
        char[] newCharSequence = new char[charSequence.length()];
        for(int i=0;i<newCharSequence.length;i++) {
            char c = charSequence.charAt(i);
            NucleotideState state = Nucleotides.getState(c);
            if (state!=null) {
                char complement = Nucleotides.getComplementaryState(state).getCode().charAt(0);
                newCharSequence[i]= Character.isUpperCase(c) ? Character.toUpperCase(complement) : Character.toLowerCase(complement);
            }
            else {
                throw new IllegalArgumentException("Invalid state "+c);
            }
        }
        newSequence.setSequenceAndAnnotations(new String(newCharSequence),newSequence.getSequenceAnnotations());
        newSequence.setName(SequenceExtractionUtilities.getComplementWithoutReverseSequenceName(name));
        return (NucleotideSequenceDocument) newSequence;
    }

}
