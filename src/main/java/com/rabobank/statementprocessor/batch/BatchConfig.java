package com.rabobank.statementprocessor.batch;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.rabobank.statementprocessor.common.ErrorMessages;
import com.rabobank.statementprocessor.common.SupportedFileType;
import com.rabobank.statementprocessor.entity.CustomerRecord;
import com.rabobank.statementprocessor.entity.CustomerRecords;
import com.rabobank.statementprocessor.exception.BusinessOperationException;
import com.rabobank.statementprocessor.reader.FlatFileReader;
import com.rabobank.statementprocessor.reader.XmlFileReader;

@Configuration
@EnableBatchProcessing
public class BatchConfig {

	Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private JobBuilderFactory jobBuilderFactory;

	@Autowired
	private StepBuilderFactory stepBuilderFactory;

	@Value("${filepath}")
	String filepath;

	@Autowired
	ErrorMessages errorMessages;

	@SuppressWarnings("unused")
	@Bean
	public Job readFilesJob() throws BusinessOperationException {
		JobBuilder builder = jobBuilderFactory.get("readFilesJob")
				.incrementer(new RunIdIncrementer())
				.listener(new BatchJobCompletionListener());

		logger.info("<inputFilePath>" + filepath);
		if (filepath == null || "".equals(filepath)) {
			logger.error(errorMessages.getNoFilePath());
			throw new BusinessOperationException(errorMessages.getNoFilePath());
		}
		File inputFile = new File(filepath);

		if (inputFile.isFile()
				&& inputFile.getName().endsWith(
						SupportedFileType.CSV.getFileType())) {
			return builder.start(step1()).build();
		} else if (inputFile.isFile()
				&& inputFile.getName().endsWith(
						SupportedFileType.XML.getFileType())) {
			return builder.start(step2()).build();
		} else {
			logger.error(errorMessages.getIrrelevantFile());
			throw new BusinessOperationException(
					errorMessages.getIrrelevantFile());
		}
	}

	@Bean
	public Step step1() {
		return stepBuilderFactory.get("step1")
				.<CustomerRecord, CustomerRecord> chunk(20)
				.reader(new FlatFileReader(filepath)).writer(writer()).build();
	}

	@Bean
	public Step step2() {
		return stepBuilderFactory.get("step2")
				.<CustomerRecords, CustomerRecords> chunk(20)
				.reader(new XmlFileReader(filepath))
				.writer(writerForCollection()).build();
	}

	@Bean
	public CsvConsoleWriter<CustomerRecord> writer() {
		return new CsvConsoleWriter<CustomerRecord>();
	}

	@Bean
	public XmlConsoleWriter<CustomerRecords> writerForCollection() {
		return new XmlConsoleWriter<CustomerRecords>();
	}
}
