package event

import "log"

type Handler func(evt interface{})

type Bus struct {
	handlers map[string][]Handler
}

func NewBus() *Bus {
	return &Bus{handlers: make(map[string][]Handler)}
}

func (b *Bus) Register(eventType string, h Handler) {
	b.handlers[eventType] = append(b.handlers[eventType], h)
}

func (b *Bus) Publish(eventType string, evt interface{}) {
	for _, h := range b.handlers[eventType] {
		func() {
			defer func() {
				if r := recover(); r != nil {
					log.Printf("event handler panic: %v", r)
				}
			}()
			h(evt)
		}()
	}
}

const (
	EventMyBlogSave             = "MyBlogSave"
	EventStatusMessageEdit      = "StatusMessageEdit"
	EventSubscriptionsBlogVisit = "SubscriptionsBlogVisit"
	EventSubscriptionsPostVisit = "SubscriptionsPostVisit"
)
