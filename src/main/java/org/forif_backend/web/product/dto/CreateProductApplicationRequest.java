package org.forif_backend.web.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.forif_backend.application.product.dto.CreateProductApplicationCommand;
import org.forif_backend.domain.product.ProductSourceType;
import org.hibernate.validator.constraints.Length;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class CreateProductApplicationRequest {

    @NotBlank
    @Length(max = 100)
    private String name;

    @NotBlank
    @Length(max = 30)
    private String slug;

    @NotBlank
    @Length(max = 200)
    private String oneLiner;

    @NotBlank
    @Length(max = 2000, message = "상세 소개는 2000자 이내로 작성해주세요.")
    private String description;

    @NotNull
    private ProductSourceType sourceType;

    @Length(max = 300)
    private String serviceUrl;

    @Length(max = 300)
    private String githubUrl;

    @Size(max = 10)
    private List<String> techStack;

    @Size(max = 10)
    private List<String> tags;

    public CreateProductApplicationCommand toCommand() {
        return new CreateProductApplicationCommand(
                name, slug, oneLiner, description, sourceType,
                serviceUrl, githubUrl, techStack, tags
        );
    }
}
