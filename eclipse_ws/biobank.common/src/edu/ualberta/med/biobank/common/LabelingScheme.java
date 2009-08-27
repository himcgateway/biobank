package edu.ualberta.med.biobank.common;

import org.springframework.util.Assert;

import edu.ualberta.med.biobank.model.Capacity;
import edu.ualberta.med.biobank.model.ContainerLabelingScheme;
import edu.ualberta.med.biobank.model.ContainerPosition;
import edu.ualberta.med.biobank.model.ContainerType;

public class LabelingScheme {

    private static final String CBSR_LABELLING_PATTERN = "ABCDEFGHJKLMNPQRSTUVWXYZ";

    private static final String SBS_ROW_LABELLING_PATTERN = "ABCDEFGH";

    /**
     * Get the rowColPos corresponding to the given sbs standard 2 char string
     * position. Use the 2 last character in case we have a full position string
     * (01AA01A2)
     */
    public static RowColPos sbsToRowCol(String pos) throws Exception {
        if (pos.length() != 2) {
            throw new Exception("binPos has an invalid length: " + pos);
        }
        int len = pos.length();
        return new RowColPos(SBS_ROW_LABELLING_PATTERN.indexOf(pos
            .charAt(len - 2)), pos.charAt(len - 1));
    }

    /**
     * Get the string corresponding to the given RowColPos and using the SBS
     * standard
     */
    public static String RowColToSBS(RowColPos rcp) {
        return "" + SBS_ROW_LABELLING_PATTERN.charAt(rcp.row) + rcp.col;
    }

    /**
     * Get the index corresponding to the given label, using the CBSR labeling
     * Use the 2 last character in case we have a full position string
     * (01AA01A2).
     */
    private static int cbsrTwoCharToInt(String label) {
        int len = label.length();
        return CBSR_LABELLING_PATTERN.indexOf(label.charAt(len - 2)) * 24
            + CBSR_LABELLING_PATTERN.indexOf(label.charAt(len - 1));
    }

    /**
     * get the RowColPos in the given container type corresponding to the given
     * label using the CBSR labeling. Use the 2 last character in case we have a
     * full position string (01AA01A2)
     */
    public static RowColPos cbsrTwoCharToRowCol(ContainerType containerType,
        String label) throws Exception {
        Integer rowCap = containerType.getCapacity().getDimensionOneCapacity();
        Integer colCap = containerType.getCapacity().getDimensionTwoCapacity();
        return cbsrTwoCharToRowCol(label, rowCap, colCap, containerType
            .getName());
    }

    /**
     * get the RowColPos in the given container corresponding to the given label
     * using the CBSR labeling. Use the 2 last character in case we have a full
     * position string (01AA01A2)
     */
    public static RowColPos cbsrTwoCharToRowCol(String label, int rowCap,
        int colCap, String containerTypeName) throws Exception {
        int pos = cbsrTwoCharToInt(label);
        if (pos >= rowCap * colCap) {
            throw new Exception("position out of bounds: containerType/"
                + containerTypeName + " pos/" + pos + " rowCap/" + rowCap
                + " colCap/" + colCap);
        }
        RowColPos rowColPos = new RowColPos();
        rowColPos.row = pos % rowCap;
        rowColPos.col = pos / rowCap;
        return rowColPos;

    }

    /**
     * Get the RowColPos in the given container corresponding to the given label
     * using the 2 char numeric labeling.Use the 2 last character in case we
     * have a full position string (01AA01A2)
     */
    public static RowColPos twoCharNumericToRowCol(ContainerType containerType,
        String label) throws Exception {
        Integer rowCap = containerType.getCapacity().getDimensionOneCapacity();
        return twoCharNumericToRowCol(label, rowCap);
    }

    /**
     * Get the RowColPos in the given container corresponding to the given label
     * using the 2 char numeric labeling.Use the 2 last character in case we
     * have a full position string (01AA01A2)
     */
    public static RowColPos twoCharNumericToRowCol(String label, int totalRows)
        throws Exception {
        int len = label.length();
        int pos = Integer.parseInt(label.substring(len - 2)) - 1;
        // has remove 1 because the two char numeric starts at 1
        RowColPos rowColPos = new RowColPos();
        rowColPos.row = pos % totalRows;
        rowColPos.col = pos / totalRows;
        return rowColPos;
    }

    /**
     * convert a position in row*column to two letter (in the CBSR way)
     */
    public static String rowColToCBSRTwoChar(RowColPos rcp, Capacity capacity) {
        int totalRows = capacity.getDimensionOneCapacity();
        int totalCols = capacity.getDimensionTwoCapacity();
        return rowColToCBSRTwoChar(rcp, totalRows, totalCols);
    }

    /**
     * convert a position in row*column to two letter (in the CBSR way)
     */
    public static String rowColToCBSRTwoChar(RowColPos rcp, int totalRows,
        int totalCols) {
        int pos1, pos2, index;
        if (totalRows == 1) {
            index = rcp.col;
        } else if (totalCols == 1) {
            index = rcp.row;
        } else {
            index = totalRows * rcp.col + rcp.row;
        }

        Assert.isTrue(index < 24 * 24);
        pos1 = index / 24;
        pos2 = index % 24;

        return String.valueOf(CBSR_LABELLING_PATTERN.charAt(pos1))
            + String.valueOf(CBSR_LABELLING_PATTERN.charAt(pos2));
    }

    /**
     * Convert a position in row*column to two char numeric.
     */
    public static String rowColToTwoCharNumeric(RowColPos rcp, Capacity capacity) {
        int totalRows = capacity.getDimensionOneCapacity();
        return rowColToTwoCharNumeric(rcp, totalRows);
    }

    /**
     * Convert a position in row*column to two char numeric.
     */
    public static String rowColToTwoCharNumeric(RowColPos rcp, int totalRows) {
        return String.format("%02d", rcp.row + totalRows * rcp.col + 1);
    }

    /**
     * Get the 2 char string corresponding to a RowColPos position inside the
     * given containerType
     */
    public static String getPositionString(RowColPos rcp,
        ContainerType containerType) {
        ContainerLabelingScheme scheme = containerType.getChildLabelingScheme();
        Capacity capacity = containerType.getCapacity();
        switch (scheme.getId()) {
        case 1:
            // SBS standard
            return RowColToSBS(rcp);
        case 2:
            // CBSR 2 char alphabetic
            return rowColToCBSRTwoChar(rcp, capacity);
        case 3:
            // 2 char numeric
            return rowColToTwoCharNumeric(rcp, capacity);
        }
        return null;
    }

    /**
     * Get the 2 char string corresponding to the given position
     */
    public static String getPositionString(ContainerPosition position) {
        RowColPos rcp = new RowColPos();
        rcp.row = position.getPositionDimensionOne();
        rcp.col = position.getPositionDimensionTwo();
        return getPositionString(rcp, position.getParentContainer()
            .getContainerType());
    }

    /**
     * Get the RowColPos position corresponding to the 2 char string position
     * inside the given container type
     */
    public static RowColPos getRowColFromPositionString(String position,
        ContainerType containerType) throws Exception {
        ContainerLabelingScheme scheme = containerType.getChildLabelingScheme();
        switch (scheme.getId()) {
        case 1:
            // SBS standard
            return sbsToRowCol(position);
        case 2:
            // CBSR 2 char alphabetic
            return cbsrTwoCharToRowCol(containerType, position);
        case 3:
            // 2 char numeric
            return twoCharNumericToRowCol(containerType, position);
        }
        return null;
    }

    public static void main(String[] args) throws Exception {
        testCBSR();
        testTwoCharNumeric();
    }

    private static void testTwoCharNumeric() throws Exception {
        int totalRows = 6;
        RowColPos rcp = new RowColPos(5, 0);
        System.out.println("Two char numeric: " + rcp.row + ":" + rcp.col
            + "=>" + rowColToTwoCharNumeric(rcp, totalRows));

        String label = "10";
        rcp = twoCharNumericToRowCol(label, totalRows);
        System.out.println("Two char numeric: " + label + "=>" + rcp.row + ":"
            + rcp.col);

    }

    private static void testCBSR() throws Exception {
        // In a 3*5 container, 1:4=AL
        int totalRows = 4;
        int totalCols = 5;

        String cbsrString = "AL";
        RowColPos rcp = cbsrTwoCharToRowCol(cbsrString, totalRows, totalCols,
            "test");
        System.out.println("CBSR: " + cbsrString + "=>" + rcp.row + ":"
            + rcp.col + " in a " + totalRows + "*" + totalCols + " container");

        rcp = new RowColPos(1, 3);
        System.out.println("CBSR: " + rcp.row + ":" + rcp.col + "=>"
            + rowColToCBSRTwoChar(rcp, totalRows, totalCols) + " in a "
            + totalRows + "*" + totalCols + " container");
    }

}
