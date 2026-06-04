package com.spotz.domain.wishlist;

import com.spotz.domain.member.Member;
import com.spotz.domain.member.MemberRepository;
import com.spotz.domain.spot.Spot;
import com.spotz.domain.spot.SpotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final MemberRepository memberRepository;
    private final SpotRepository spotRepository;

    public List<WishResponse> getMyWishlist(Long memberId) {
        return wishlistRepository.findByMemberMemberIdOrderByCreatedAtDesc(memberId)
                .stream().map(WishResponse::from).collect(Collectors.toList());
    }

    @Transactional
    public void toggle(Long memberId, Long spotId) {
        Optional<Wishlist> existing = wishlistRepository.findByMemberMemberIdAndSpotSpotId(memberId, spotId);
        if (existing.isPresent()) {
            wishlistRepository.delete(existing.get());
        } else {
            Member member = memberRepository.findById(memberId).orElseThrow();
            Spot spot = spotRepository.findById(spotId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 스팟입니다."));
            wishlistRepository.save(Wishlist.builder().member(member).spot(spot).build());
        }
    }
}
