package event

import (
	"database/sql"
	"log"

	"github.com/nyoongoon/closest-v2/backend-go/internal/domain/blog"
	"github.com/nyoongoon/closest-v2/backend-go/internal/domain/member"
)

func RegisterListeners(bus *Bus, memberRepo member.Repository, blogRepo blog.Repository) {
	bus.Register(EventMyBlogSave, func(evt interface{}) {
		e := evt.(MyBlogSaveEvent)
		m, err := memberRepo.FindByEmail(e.MemberEmail)
		if err != nil || m == nil {
			log.Printf("MyBlogSave: member not found: %s", e.MemberEmail)
			return
		}
		m.BlogURL = sql.NullString{String: e.BlogURL, Valid: true}
		if err := memberRepo.Update(m); err != nil {
			log.Printf("MyBlogSave: update failed: %v", err)
		}
	})

	bus.Register(EventStatusMessageEdit, func(evt interface{}) {
		e := evt.(StatusMessageEditEvent)
		b, err := blogRepo.FindByBlogURL(e.BlogURL)
		if err != nil || b == nil {
			log.Printf("StatusMessageEdit: blog not found: %s", e.BlogURL)
			return
		}
		b.StatusMessage = sql.NullString{String: e.StatusMessage, Valid: true}
		if err := blogRepo.Update(b); err != nil {
			log.Printf("StatusMessageEdit: update failed: %v", err)
		}
	})

	bus.Register(EventSubscriptionsBlogVisit, func(evt interface{}) {
		e := evt.(SubscriptionsBlogVisitEvent)
		b, err := blogRepo.FindByBlogURL(e.BlogURL)
		if err != nil || b == nil {
			log.Printf("BlogVisit: blog not found: %s", e.BlogURL)
			return
		}
		b.BlogVisitCount++
		if err := blogRepo.Update(b); err != nil {
			log.Printf("BlogVisit: update failed: %v", err)
		}
	})

	bus.Register(EventSubscriptionsPostVisit, func(evt interface{}) {
		e := evt.(SubscriptionsPostVisitEvent)
		b, err := blogRepo.FindByBlogURL(e.BlogURL)
		if err != nil || b == nil {
			log.Printf("PostVisit: blog not found: %s", e.BlogURL)
			return
		}
		b.BlogVisitCount++
		if err := blogRepo.Update(b); err != nil {
			log.Printf("PostVisit: blog update failed: %v", err)
		}
		posts, err := blogRepo.FindPostsByBlogID(b.ID)
		if err != nil {
			return
		}
		for _, p := range posts {
			if p.PostURL == e.PostURL {
				p.PostVisitCount++
				if err := blogRepo.UpdatePost(p); err != nil {
					log.Printf("PostVisit: post update failed: %v", err)
				}
				break
			}
		}
	})
}
