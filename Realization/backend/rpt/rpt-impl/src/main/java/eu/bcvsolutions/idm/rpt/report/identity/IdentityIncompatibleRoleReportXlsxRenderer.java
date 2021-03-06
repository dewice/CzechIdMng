package eu.bcvsolutions.idm.rpt.report.identity;

import java.io.IOException;
import java.io.InputStream;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import eu.bcvsolutions.idm.rpt.api.dto.RptReportDto;
import eu.bcvsolutions.idm.rpt.api.exception.ReportRenderException;
import eu.bcvsolutions.idm.rpt.api.renderer.AbstractXlsxRenderer;
import eu.bcvsolutions.idm.rpt.api.renderer.RendererRegistrar;
import eu.bcvsolutions.idm.rpt.dto.RptIdentityIncompatibleRoleDto;

/**
 * Render report of identities with incompatible roles.
 * 
 * @author Radek Tomiška
 * @since 9.4.0
 *
 */
@Component("identity-incompatible-role-report-xlsx-renderer")
@Description(AbstractXlsxRenderer.RENDERER_EXTENSION) // will be show as format for download
public class IdentityIncompatibleRoleReportXlsxRenderer extends AbstractXlsxRenderer implements RendererRegistrar {

	@Override
	public InputStream render(RptReportDto report) {
		try {
			// read json stream
			JsonParser jParser = getMapper().getFactory().createParser(getReportData(report));
			XSSFWorkbook workbook = new XSSFWorkbook();
			XSSFSheet sheet = workbook.createSheet("Report");
			sheet.setDefaultColumnWidth(15);
			// header
			Row row = sheet.createRow(0);
			Cell cell = row.createCell(0);
			cell.setCellValue("Identity - username");
			cell = row.createCell(1);
			cell.setCellValue("Identity - personal number");
			cell = row.createCell(2);
			cell.setCellValue("Identity - lastName");
			cell = row.createCell(3);
			cell.setCellValue("Identity - firstName");
			cell = row.createCell(4);
			cell.setCellValue("Assigned role");
			cell = row.createCell(5);
			cell.setCellValue("Incompatible role definition - role with definition");
			cell = row.createCell(6);
			cell.setCellValue("Incompatible role definition - target role");
		
			int rowNum = 1;
			//
			// json is array of identities
			if (jParser.nextToken() == JsonToken.START_ARRAY) {
				// write single identity
				while (jParser.nextToken() == JsonToken.START_OBJECT) {
					RptIdentityIncompatibleRoleDto item = getMapper().readValue(jParser, RptIdentityIncompatibleRoleDto.class);

					row = sheet.createRow(rowNum++);			
					cell = row.createCell(0);
					cell.setCellValue(item.getIdentity().getUsername());
					cell = row.createCell(1);
					cell.setCellValue(item.getIdentity().getExternalCode());
					cell = row.createCell(2);
					cell.setCellValue(item.getIdentity().getLastName());
					cell = row.createCell(3);
					cell.setCellValue(item.getIdentity().getFirstName());
					cell = row.createCell(4);
					cell.setCellValue(item.getDirectRole().getCode());
					cell = row.createCell(5);
					cell.setCellValue(item.getSuperior().getCode());
					cell = row.createCell(6);
					cell.setCellValue(item.getSub().getCode());
				}
			}
			// close json stream
			jParser.close();
			//
			// close and return input stream
			return getInputStream(workbook);
		} catch (IOException ex) {
			throw new ReportRenderException(report.getName(), ex);
		}
	}
	
	/**
	 * Register renderer to report
	 */
	@Override
	public String[] register(String reportName) {
		if (IdentityIncompatibleRoleReportExecutor.REPORT_NAME.equals(reportName)) {
			return new String[] { getName() };
		}
		return new String[] {};
	}

}