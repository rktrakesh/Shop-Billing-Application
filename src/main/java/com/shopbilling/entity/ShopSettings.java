package com.shopbilling.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "shop_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ShopSettings extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "shop_name", nullable = false, length = 100)
    private String shopName;

    @Column(name = "shop_address", length = 500)
    private String shopAddress;

    @Column(name = "mobile_number", length = 20)
    private String mobileNumber;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "gst_number", length = 20)
    private String gstNumber;

    @Column(name = "footer_message", length = 500)
    private String footerMessage;

    @Column(name = "logo_path", length = 500)
    private String logoPath;
}
