package com.broadridge.pdf2print.dataprep.service;

import com.broadridge.pdf2print.dataprep.models.FileCoordinate;
import com.broadridge.pdf2print.dataprep.utils.Constants;
import com.broadridge.pdf2print.dataprep.utils.TextServiceUtils;
import com.dstoutput.custsys.jafp.*;
import org.json.JSONObject;

import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.nio.ByteBuffer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextService {
    private final TextServiceUtils textServiceUtils = new TextServiceUtils();
    private CoverSheetService coverSheetService = new CoverSheetService();
    private final static String micrPattern1 = "(\\w{1})(\\d+)(\\w{2})(\\d+)(\\w{1})(\\d+)(\\w{1})";
    private final static String micrPattern2 = "(<)(\\d+)(<:)(\\d+)(:)(\\d+)(<)";

    public void removeTextAt(List<List<AfpRec>> listOflistOfPages, FileCoordinate fileCoordinates) throws Exception {
        double pageDPI = 1440.0;
        int bpgCounter = 0;

        List<Integer> validOrientations = Arrays.asList(0, 90, 180, 270);
        int configOrientation = fileCoordinates.getIOrientation();
        if (!validOrientations.contains(configOrientation)) {
            System.err.println("Invalid orientation value in configuration: " + configOrientation
                    + ". Valid values are: 0, 90, 180, 270.");
            System.exit(1);
        }

        for (List<AfpRec> listOfPages : listOflistOfPages) {
            int recPos = -1;
            for (AfpRec inputRec : listOfPages) { // 11 Size
                boolean isDeleted = false; // Reset the flag for each record
                ArrayList<AfpPtxDataItem> newPtxDataItems = new ArrayList<>();
                recPos++;

                if (inputRec.getTla().equalsIgnoreCase("BPG")) {
                    bpgCounter++;
                }

                // Only proceed when we are at the correct page
                if (bpgCounter == fileCoordinates.getPagenum()) {

                    if (inputRec.getTla().equalsIgnoreCase("PGD")) {
                        AfpCmdRaw afpCmdRaw = new AfpCmdRaw(inputRec);
                        AfpCmdType afpCmdType = afpCmdRaw.getCmdType();
                        if (afpCmdType == AfpCmdType.PGD) {
                            PrintStream printStreamOriginal = textServiceUtils.hideSystemout();
                            AfpCmdPGD pgd = ((AfpCmdPGD) inputRec.parse());
                            System.setOut(printStreamOriginal);
                            pageDPI = textServiceUtils.getDPIFromPGD(pgd);
                        }
                    }

                    if (inputRec.getTla().equalsIgnoreCase("PTX")) {
                        AfpCmdRaw afpCmdRaw = new AfpCmdRaw(inputRec);
                        AfpCmdType afpCmdType = afpCmdRaw.getCmdType();

                        if (afpCmdType == AfpCmdType.PTX) {
                            double xPos = 0;
                            double yPos = 0;
                            int currentIOrientation = 0;

                            PrintStream printStreamOriginal = textServiceUtils.hideSystemout();
                            AfpCmdPTX afpCmdPTX = ((AfpCmdPTX) inputRec.parse());
                            System.setOut(printStreamOriginal);

                            List<AfpPtxDataItem> ptxDataItems = afpCmdPTX.getDataItems();
                            createCtrlSeqPrefix(newPtxDataItems);

                            for (AfpPtxDataItem dataItem : ptxDataItems) {
                                if (dataItem instanceof AfpPtxCtrlSeq) {
                                    AfpPtxCtrlSeq newCtrlSeq = new AfpPtxCtrlSeq();
                                    AfpPtxCtrlSeq ctrlSeq = (AfpPtxCtrlSeq) dataItem;

                                    for (AfpPtxCtrl ctrl : ctrlSeq.getControls()) {
                                        AfpPtxCtrlType ctrlType = ctrl.getType();

                                        if (ctrlType == AfpPtxCtrlType.AMB) {
                                            AfpPtxCtrlAMB ambPtxCtrl = (AfpPtxCtrlAMB) ctrl;
                                            yPos = ambPtxCtrl.getDisplacement();
                                            yPos = textServiceUtils.round(yPos, 4, pageDPI);
                                        }

                                        if (ctrlType == AfpPtxCtrlType.STO) {
                                            AfpPtxCtrlSTO stoPtxCtrl = (AfpPtxCtrlSTO) ctrl;
                                            currentIOrientation = Integer.parseInt(stoPtxCtrl.getIorntion()
                                                    .degreestoEnglish().replaceAll("[^\\d]", ""));
                                            System.out.println("Current Orientation is: " + currentIOrientation);
                                        }

                                        if (ctrlType == AfpPtxCtrlType.AMI) {
                                            AfpPtxCtrlAMI amiPtxCtrl = (AfpPtxCtrlAMI) ctrl;
                                            xPos = amiPtxCtrl.getDisplacement();
                                            xPos = textServiceUtils.round(xPos, 4, pageDPI);
                                        }

                                        if (ctrlType == AfpPtxCtrlType.TRN) {
                                            // Check if inside the box and matching orientation
                                            if (textServiceUtils.isInsideTheBox(xPos, yPos, fileCoordinates)
                                                    && currentIOrientation == fileCoordinates.getIOrientation()) {
                                                AfpPtxCtrlTRN trnPtxCtrl = (AfpPtxCtrlTRN) ctrl;
                                                System.out.println("Removed TRN Rec: " + trnPtxCtrl.getTrnString());
                                                if (Constants.debug)
                                                    System.err.println("[debug] Removing from Input: XY=" + xPos + ", "
                                                            + yPos + ", Orientation=" + currentIOrientation + ", Text="
                                                            + trnPtxCtrl.getTrnString());

                                                trnPtxCtrl = new AfpPtxCtrlTRN("", true); // Clear original TRN line
                                                ctrl = trnPtxCtrl;
                                                isDeleted = true; // Mark as deleted
                                            }
                                        }

                                        newCtrlSeq.addCtrl(ctrl);
                                    }
                                    newPtxDataItems.add(newCtrlSeq);
                                }
                            }
                        }
                    }
                }

                if (isDeleted) {
                    AfpRec rec = new AfpCmdPTX(newPtxDataItems).toAfpRec((short) 0, 0);
                    listOfPages.set(recPos, rec); // Update the record
                }
            }
        }
    }

    public boolean checkForMICR(List<AfpRec> page, List<FileCoordinate> fileCoordinates) throws Exception {
        double pageDPI = 300.0;
        StringBuilder textLine = new StringBuilder();
        for (AfpRec inputRec : page) {
            AfpCmdRaw afpCmdRaw = new AfpCmdRaw(inputRec);
            AfpCmdType afpCmdType = afpCmdRaw.getCmdType();
            if (afpCmdType == AfpCmdType.PGD) {
                PrintStream printStreamOriginal = textServiceUtils.hideSystemout();
                AfpCmdPGD pgd = ((AfpCmdPGD) inputRec.parse());
                System.setOut(printStreamOriginal);
                pageDPI = textServiceUtils.getDPIFromPGD(pgd);
                break;
            }
        }

        for (AfpRec inputRec : page) {
            AfpCmdRaw afpCmdRaw = new AfpCmdRaw(inputRec);
            AfpCmdType afpCmdType = afpCmdRaw.getCmdType();
            if (afpCmdType == AfpCmdType.PTX) {
                double xPos = 0;
                double yPos = 0;
                int currentIOrientation = 0;

                PrintStream printStreamOriginal = textServiceUtils.hideSystemout();
                AfpCmdPTX afpCmdPTX = ((AfpCmdPTX) inputRec.parse());
                System.setOut(printStreamOriginal);

                ArrayList<AfpPtxDataItem> newPtxDataItems = new ArrayList<>();
                List<AfpPtxDataItem> ptxDataItems = afpCmdPTX.getDataItems();
                createCtrlSeqPrefix(newPtxDataItems);// Must include this in order to view PTX data

                for (AfpPtxDataItem dataItem : ptxDataItems) {
                    if (dataItem instanceof AfpPtxCtrlSeq) {
                        AfpPtxCtrlSeq ctrlSeq = (AfpPtxCtrlSeq) dataItem;

                        for (AfpPtxCtrl ctrl : ctrlSeq.getControls()) {
                            AfpPtxCtrlType ctrlType = ctrl.getType();
                            if (ctrlType == AfpPtxCtrlType.AMB) {
                                AfpPtxCtrlAMB ambPtxCtrl = (AfpPtxCtrlAMB) ctrl;
                                xPos = ambPtxCtrl.getDisplacement();
                                xPos = textServiceUtils.round(xPos, 4, pageDPI);
                            }
                            if (ctrlType == AfpPtxCtrlType.STO) {
                                AfpPtxCtrlSTO stoPtxCtrl = (AfpPtxCtrlSTO) ctrl;
                                currentIOrientation = stoPtxCtrl.getIorntion().asInt();
                            }
                            if (ctrlType == AfpPtxCtrlType.AMI) {
                                AfpPtxCtrlAMI amiPtxCtrl = (AfpPtxCtrlAMI) ctrl;
                                yPos = amiPtxCtrl.getDisplacement();
                                yPos = textServiceUtils.round(yPos, 4, pageDPI);
                            }
                            if (ctrlType == AfpPtxCtrlType.TRN) {
                                for (FileCoordinate fileCoordinate : fileCoordinates) {
                                    if (textServiceUtils.isInsideTheBox(xPos, yPos, fileCoordinate)
                                            && currentIOrientation == fileCoordinate.getIOrientation()) {
                                        AfpPtxCtrlTRN trnPtxCtrl = (AfpPtxCtrlTRN) ctrl;
                                        String txt = convertMICR(trnPtxCtrl.getTrnString()).toString();
                                        textLine.append(txt);
                                        if (isMICRpattern(textLine)) {
                                            textLine.setLength(0);
                                            return true;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    public void updateMICR(List<AfpRec> page, List<FileCoordinate> fileCoordinates) throws Exception {
        int recPos = -1;
        double pageDPI = 300.0;

        double micrXPos = Integer.MAX_VALUE;
        double micrYPos = Integer.MAX_VALUE;

        for (AfpRec inputRec : page) {
            AfpCmdRaw afpCmdRaw = new AfpCmdRaw(inputRec);
            AfpCmdType afpCmdType = afpCmdRaw.getCmdType();
            if (afpCmdType == AfpCmdType.PGD) {
                PrintStream printStreamOriginal = textServiceUtils.hideSystemout();
                AfpCmdPGD pgd = ((AfpCmdPGD) inputRec.parse());
                System.setOut(printStreamOriginal);
                pageDPI = textServiceUtils.getDPIFromPGD(pgd);
                break;
            }
        }

        boolean foundMICR = false;
        int micrPTXPos = -1;
        StringBuilder textLine = new StringBuilder();
        for (AfpRec inputRec : page) {
            recPos++;
            AfpCmdRaw afpCmdRaw = new AfpCmdRaw(inputRec);
            AfpCmdType afpCmdType = afpCmdRaw.getCmdType();
            if (afpCmdType == AfpCmdType.PTX) {
                double xPos = 0;
                double yPos = 0;
                boolean isUpdated = false;
                int currentIOrientation = 0;

                PrintStream printStreamOriginal = textServiceUtils.hideSystemout();
                AfpCmdPTX afpCmdPTX = ((AfpCmdPTX) inputRec.parse());
                System.setOut(printStreamOriginal);

                ArrayList<AfpPtxDataItem> newPtxDataItems = new ArrayList<>();
                List<AfpPtxDataItem> ptxDataItems = afpCmdPTX.getDataItems();
                createCtrlSeqPrefix(newPtxDataItems);// Must include this in order to view PTX data

                for (AfpPtxDataItem dataItem : ptxDataItems) {
                    if (dataItem instanceof AfpPtxCtrlSeq) {
                        AfpPtxCtrlSeq newCtrlSeq = new AfpPtxCtrlSeq();
                        AfpPtxCtrlSeq ctrlSeq = (AfpPtxCtrlSeq) dataItem;

                        for (AfpPtxCtrl ctrl : ctrlSeq.getControls()) {
                            AfpPtxCtrlType ctrlType = ctrl.getType();
                            if (ctrlType == AfpPtxCtrlType.AMB) {
                                AfpPtxCtrlAMB ambPtxCtrl = (AfpPtxCtrlAMB) ctrl;
                                xPos = ambPtxCtrl.getDisplacement();
                                xPos = textServiceUtils.round(xPos, 4, pageDPI);
                            }
                            if (ctrlType == AfpPtxCtrlType.STO) {
                                AfpPtxCtrlSTO stoPtxCtrl = (AfpPtxCtrlSTO) ctrl;
                                currentIOrientation = stoPtxCtrl.getIorntion().asInt();
                            }
                            if (ctrlType == AfpPtxCtrlType.AMI) {
                                AfpPtxCtrlAMI amiPtxCtrl = (AfpPtxCtrlAMI) ctrl;
                                yPos = amiPtxCtrl.getDisplacement();
                                yPos = textServiceUtils.round(yPos, 4, pageDPI);
                            }
                            if (ctrlType == AfpPtxCtrlType.TRN) {
                                for (FileCoordinate fileCoordinate : fileCoordinates) {
                                    if (textServiceUtils.isInsideTheBox(xPos, yPos, fileCoordinate)
                                            && currentIOrientation == fileCoordinate.getIOrientation()) {
                                        micrXPos = Math.min(xPos, micrXPos);
                                        micrYPos = Math.min(yPos, micrYPos);
                                        AfpPtxCtrlTRN trnPtxCtrl = (AfpPtxCtrlTRN) ctrl;

                                        if (Constants.debug)
                                            System.err.println("[debug] updating Input: XY=" + xPos + ", " + yPos
                                                    + ", Orientation=" + currentIOrientation + ", Text="
                                                    + trnPtxCtrl.getTrnString());

                                        String txt = convertMICR(trnPtxCtrl.getTrnString()).toString();
                                        textLine.append(txt);

                                        trnPtxCtrl = new AfpPtxCtrlTRN("", true);// clear original TRN line
                                        ctrl = trnPtxCtrl;
                                        isUpdated = true;
                                    }
                                }
                            }
                            newCtrlSeq.addCtrl(ctrl);
                            if (isMICRpattern(textLine)) {
                                foundMICR = true;
                                micrPTXPos = recPos;
                            }
                        }
                        newPtxDataItems.add(newCtrlSeq);
                    }
                }

                if (isUpdated) {
                    AfpRec rec = new AfpCmdPTX(newPtxDataItems).toAfpRec((short) 0, 0);
                    page.set(recPos, rec);
                }
            }
        }

        if (foundMICR) {
            int mcf1Pos = getNextFontIdOrMCF1Pos(page, false);
            int nxtFontId = getNextFontIdOrMCF1Pos(page, true);

            JSONObject micrLine = DataprepLoader.dataPrepConfig.getJSONObject("MICR_LINE_PLACEMENT");
            micrYPos = micrLine.getDouble("x_pos");
            micrXPos = micrLine.getDouble("y_pos");

//            System.out.println("MCF : " + mcf1Pos + " " +nxtFontId + " " + micrYPos + " " + micrXPos);

            page.add(mcf1Pos - 1, getMICRMCFRec(nxtFontId));

            if (micrPTXPos != -1) {
                double moveToLeft = 0.0416; // Moving MICR to left
                AfpRec afpRec = coverSheetService.createPTXRecord(
                        numToDP(String.valueOf(textServiceUtils.round(micrYPos - moveToLeft, 4, 1)), pageDPI),
                        numToDP(String.valueOf(micrXPos), pageDPI), textLine.toString(), (short) nxtFontId);
                page.add(micrPTXPos + 1, afpRec);
                textLine.setLength(0);
            }
        }
    }

    private StringBuilder convertMICR(String str) {
        String replaceString = str.replace('z', '<');
        String replaceString2 = replaceString.replace('t', ':');
        String replaceString3 = replaceString2.replace('C', '<');
        String replaceString4 = replaceString3.replace('A', ':');

        return new StringBuilder(replaceString4);
    }

    private short numToDP(String coordinate, double pageDPI) {
        double coordinateValue = Double.parseDouble(coordinate) * pageDPI;
        return (short) coordinateValue;
    }

    private AfpRec getMICRMCFRec(int nxtFontId) throws Exception {
        // MICR FONT C0MDI0B0_T1E4MICR
        String font_code = "C0MDI0B0";
        String font_code_2 = "T1E4MICR";
        return addCmdMCF(font_code, font_code_2, new UBIN1(nxtFontId));
    }

    private int getNextFontIdOrMCF1Pos(List<AfpRec> page, boolean nextFont) {
        int nxtFontId = -1;
        int recPos = -1;
        for (int idx = 0; idx < page.size(); idx++) {
            AfpRec inputRec = page.get(idx);
            AfpCmdRaw afpCmdRaw2 = new AfpCmdRaw(inputRec);
            AfpCmdType afpCmdType2 = afpCmdRaw2.getCmdType();
            if (afpCmdType2 == AfpCmdType.MCF1) {
                try {
                    AfpCmdMCF1 afpCmdMCF = ((AfpCmdMCF1) inputRec.parse());
                    int fontSize = afpCmdMCF.getResourceHash().size();
                    nxtFontId = fontSize + 1;
                    recPos = idx;
                    break;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return nextFont ? nxtFontId : recPos;
    }

    private Boolean isMICRpattern(StringBuilder textLine) {
        Boolean foundMICR = false;
        if (textLine.length() <= 0) {
            return foundMICR;
        }
        String str = textLine.toString().replaceAll("\\u00F8", "");
        Pattern linePattern = Pattern.compile(micrPattern1, Pattern.CASE_INSENSITIVE);
        Pattern linePattern2 = Pattern.compile(micrPattern2, Pattern.CASE_INSENSITIVE);
        Matcher lineMatcher = linePattern.matcher(str);
        Matcher lineMatcher2 = linePattern2.matcher(str);
        if (lineMatcher.matches() || lineMatcher2.matches()) {
            foundMICR = true;
        }
        return foundMICR;
    }

    public AfpRec addCmdMCF(String binaryName, String fqName, UBIN1 resID) throws Exception {
        UBIN2 rglength = new UBIN2(0x0025);

        UBIN1 fqntype = new UBIN1(0x86);
        AfpByteString FQName = new AfpByteString(binaryName); // from coversheet.cfg "C0ARU080"
        AfpTriplet02 afptriplet = new AfpTriplet02(fqntype, FQName);

        UBIN1 fqntype1 = new UBIN1(0x85);
        AfpByteString FQName1 = new AfpByteString(fqName); // from coversheet.cfg "T1E4LAT1"
        AfpTriplet02 afptriplet2 = new AfpTriplet02(fqntype1, FQName1);

        UBIN1 restype = new UBIN1(0x05);
        AfpTriplet24 afptriplet24 = new AfpTriplet24(restype, resID);

        UBIN1 ResSNum = new UBIN1(0x00);
        AfpTriplet25 afptriplet25 = new AfpTriplet25(ResSNum);

        UBIN2 CharRot = new UBIN2(0x00);
        AfpTriplet26 afptriplet26 = new AfpTriplet26(CharRot);
        AfpByteArrayList bodyBytesList = new AfpByteArrayList();

        bodyBytesList.add(rglength.toBytes());
        bodyBytesList.add(afptriplet.toBytes());
        bodyBytesList.add(afptriplet2.toBytes());
        bodyBytesList.add(afptriplet24.toBytes());
        bodyBytesList.add(afptriplet25.toBytes());
        bodyBytesList.add(afptriplet26.toBytes());
        byte[] bodyBytes = bodyBytesList.toBytes();

        AfpSFIntro sfIntro = new AfpSFIntro(157, 0xD3AB8A, (short) 0, 0);
        AfpRec mcfCmdRaw = new AfpRec(AfpRec.START_MARKER, sfIntro, bodyBytes);

        AfpCmdRaw mcfCmdRawRec = new AfpCmdRaw(mcfCmdRaw);
        AfpCmdMCF mcfRec = new AfpCmdMCF(mcfCmdRawRec);

        return mcfRec.toAfpRec((short) 0, 0);
    }

    private void createCtrlSeqPrefix(ArrayList<AfpPtxDataItem> newPtxDataItems) {
        // Integer zeroByte = new Integer(0);
        Integer seqPrefix = new Integer(AfpCmdPTX.CTRL_SEQ_PREFIX);
        Integer seqClass = new Integer(AfpCmdPTX.CTRL_SEQ_CLASS);
        // byte[] startBytes = { zeroByte.byteValue(), zeroByte.byteValue(),
        // zeroByte.byteValue(), seqPrefix.byteValue(),
        // seqClass.byteValue() };
        byte[] startBytes = { seqPrefix.byteValue(), seqClass.byteValue() };
        ByteBuffer escapeChars = ByteBuffer.wrap(startBytes);
        AfpPtxCharSeq escapeCharsSeq = new AfpPtxCharSeq(escapeChars);
        newPtxDataItems.add(escapeCharsSeq);
    }

    public List<String> extractTextAt(List<AfpRec> page, List<FileCoordinate> fileCoordinates) throws Exception {
        int recPos = -1;
        double pageDPI = 1440.0;
        ArrayList lines = new ArrayList<>();

        for (AfpRec inputRec : page) {
            AfpCmdRaw afpCmdRaw = new AfpCmdRaw(inputRec);
            AfpCmdType afpCmdType = afpCmdRaw.getCmdType();
            if (afpCmdType == AfpCmdType.PGD) {
                PrintStream printStreamOriginal = textServiceUtils.hideSystemout();
                AfpCmdPGD pgd = ((AfpCmdPGD) inputRec.parse());
                System.setOut(printStreamOriginal);
                pageDPI = textServiceUtils.getDPIFromPGD(pgd);
                System.out.println("DPI from PGD:" + pageDPI);
                break;
            }
        }

        for (AfpRec inputRec : page) {
            recPos++;
            AfpCmdRaw afpCmdRaw = new AfpCmdRaw(inputRec);
            AfpCmdType afpCmdType = afpCmdRaw.getCmdType();
            if (afpCmdType == AfpCmdType.PTX) {
                double xPos = 0;
                double yPos = 0;
                double pYpos = 0;
                double pXpos = 0;
                boolean isDeleted = false;
                int currentIOrientation = 0;
                int currentIOrientation2 = 0;
                String textLine = "";

                String eachLine = "";

                PrintStream printStreamOriginal = textServiceUtils.hideSystemout();
                AfpCmdPTX afpCmdPTX = ((AfpCmdPTX) inputRec.parse());
                System.setOut(printStreamOriginal);

                ArrayList<AfpPtxDataItem> newPtxDataItems = new ArrayList<>();
                List<AfpPtxDataItem> ptxDataItems = afpCmdPTX.getDataItems();
                createCtrlSeqPrefix(newPtxDataItems);// Must include this in order to view PTX data

                for (AfpPtxDataItem dataItem : ptxDataItems) {
                    if (dataItem instanceof AfpPtxCtrlSeq) {
                        AfpPtxCtrlSeq newCtrlSeq = new AfpPtxCtrlSeq();
                        AfpPtxCtrlSeq ctrlSeq = (AfpPtxCtrlSeq) dataItem;

                        for (AfpPtxCtrl ctrl : ctrlSeq.getControls()) {
                            AfpPtxCtrlType ctrlType = ctrl.getType();
                            if (ctrlType == AfpPtxCtrlType.AMB) {
                                AfpPtxCtrlAMB ambPtxCtrl = (AfpPtxCtrlAMB) ctrl;
                                yPos = ambPtxCtrl.getDisplacement();
                                yPos = textServiceUtils.round(yPos, 4, pageDPI);
                                // System.out.println("AMB yPos " + yPos);

                            }
                            if (ctrlType == AfpPtxCtrlType.STO) {
                                AfpPtxCtrlSTO stoPtxCtrl = (AfpPtxCtrlSTO) ctrl;
                                currentIOrientation = Integer.parseInt(stoPtxCtrl.getIorntion()
                                        .degreestoEnglish().replaceAll("[^\\d]", ""));
                                // currentIorientation2 = stoPtxCtrl.getBorntion().asInt();
                                // System.err.println("STO " + stoPtxCtrl.toEnglish());
                                // System.out.println("IOren " + stoPtxCtrl.getBorntion().asInt());
                                // System.out.println("Boren " + stoPtxCtrl.getIorntion().asInt());
                            }
                            if (ctrlType == AfpPtxCtrlType.AMI) {
                                AfpPtxCtrlAMI amiPtxCtrl = (AfpPtxCtrlAMI) ctrl;
                                xPos = amiPtxCtrl.getDisplacement();
                                xPos = textServiceUtils.round(xPos, 4, pageDPI);
                                // System.out.println("AMI xPos " + xPos);
                                if (textLine.length() > 0) {
                                    textLine = textLine.concat(" ");
                                }
                            }
                            if (ctrlType == AfpPtxCtrlType.TRN) {
                                for (FileCoordinate fileCoordinate : fileCoordinates) {
                                    if (textServiceUtils.isInsideTheBox(xPos, yPos, fileCoordinate)
                                            && currentIOrientation == fileCoordinate.getIOrientation()) {
                                        AfpPtxCtrlTRN trnPtxCtrl = (AfpPtxCtrlTRN) ctrl;
                                        // System.out.println("[debug] Removing from Input: XY=" + xPos + ", " + yPos +
                                        // ", Orientation=" + currentIOrientation + ", Text="+
                                        // trnPtxCtrl.getTrnString());
                                       

                                        if (pYpos == 0 || pYpos == yPos) {
                                            textLine = textLine.concat(trnPtxCtrl.getTrnString());
                                        } else {
                                            eachLine = textLine;
                                            lines.add(eachLine.trim());
                                            textLine = trnPtxCtrl.getTrnString();
                                        }

                                        pYpos = yPos;
                                        pXpos = xPos;
                                    }

                                }
                            }

                            newCtrlSeq.addCtrl(ctrl);
                        }
                        newPtxDataItems.add(newCtrlSeq);
                    }

                }
                if (textLine.length() > 0) // Add the last line to the list
                {
                    eachLine = textLine;
                    lines.add(eachLine.trim());
                }
                if (!lines.isEmpty())
                    System.out.println("Line Array" + lines.toString());
                if (isDeleted) {
//                    System.err.println(newPtxDataItems.get(0).toEnglish());
                    AfpRec rec = new AfpCmdPTX(newPtxDataItems).toAfpRec((short) 0, 0);
                    page.set(recPos, rec);
                }

            }

        }

        return lines;
    }

}

