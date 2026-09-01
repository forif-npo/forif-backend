package org.forif_backend.domain.product;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.forif_backend.common.BaseTimeEntity;

/**
 * 서비스 팀원.
 * 졸업생 등 비가입자도 있을 수 있어 유저 FK 대신 이름을 직접 보관한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "tb_product_member")
public class ProductMember extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_member_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "user_name", nullable = false, length = 50)
    private String userName;

    /** 역할 표기 (예: "팀장 · 백엔드") */
    @Column(name = "role_label", nullable = false, length = 50)
    private String roleLabel;

    public static ProductMember create(Product product, String userName, String roleLabel) {
        ProductMember member = new ProductMember();
        member.product = product;
        member.userName = userName;
        member.roleLabel = roleLabel;
        return member;
    }
}
